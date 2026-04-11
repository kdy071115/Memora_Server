package com.kit.memora_server.domain.conceptgraph.controller;

import com.kit.memora_server.domain.conceptgraph.dto.ConceptGraphResponse;
import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.lecture.repository.LectureRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.global.security.CustomUserDetails;
import com.kit.memora_server.infra.ai.AiServerClient;
import com.kit.memora_server.infra.ai.dto.AiConceptGraphRequest;
import com.kit.memora_server.infra.ai.dto.AiConceptGraphResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ConceptGraph", description = "강의 자료 → 개념 지식 그래프")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ConceptGraphController {

    private final AiServerClient aiServerClient;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Operation(summary = "차시의 개념 지식 그래프 생성",
            description = "강의 자료에서 핵심 개념 8-20개와 그 관계(선행/포함/관련)를 추출합니다.")
    @GetMapping("/api/lectures/{lectureId}/concept-graph")
    public ResponseEntity<ApiResponse<ConceptGraphResponse>> get(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user) {

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        Long courseId = lecture.getCourse().getId();
        boolean isInstructor = lecture.getCourse().getInstructor().getId().equals(user.getId());
        if (!isInstructor && !enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        AiConceptGraphResponse aiRes = aiServerClient.generateConceptGraph(
                AiConceptGraphRequest.builder().lectureId(lectureId).build()
        );

        List<ConceptGraphResponse.Node> nodes = aiRes.getNodes() == null
                ? List.of()
                : aiRes.getNodes().stream()
                    .map(n -> ConceptGraphResponse.Node.builder()
                            .id(n.getId())
                            .label(n.getLabel())
                            .importance(n.getImportance())
                            .build())
                    .toList();

        List<ConceptGraphResponse.Edge> edges = aiRes.getEdges() == null
                ? List.of()
                : aiRes.getEdges().stream()
                    .map(e -> ConceptGraphResponse.Edge.builder()
                            .source(e.getSource())
                            .target(e.getTarget())
                            .label(e.getLabel())
                            .build())
                    .toList();

        return ResponseEntity.ok(ApiResponse.ok(
                ConceptGraphResponse.builder().nodes(nodes).edges(edges).build()
        ));
    }
}
