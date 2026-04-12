package com.kit.memora_server.domain.learning.controller;

import com.kit.memora_server.domain.learning.dto.SelfExplainHistoryItem;
import com.kit.memora_server.domain.learning.dto.SelfExplainRequest;
import com.kit.memora_server.domain.learning.dto.SelfExplainResponse;
import com.kit.memora_server.domain.learning.service.SelfExplainService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "SelfExplain", description = "자기 설명 평가 (메타인지 학습)")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SelfExplainController {

    private final SelfExplainService selfExplainService;

    @Operation(summary = "자기 설명 평가",
            description = "학생이 강의 자료를 본 뒤 본인 말로 작성한 설명을 평가합니다. " +
                    "잘 이해한 부분 / 빠뜨린 개념 / 오개념 / 다음 단계를 진단합니다.")
    @PostMapping("/api/lectures/{lectureId}/self-explain")
    public ResponseEntity<ApiResponse<SelfExplainResponse>> evaluate(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody SelfExplainRequest request) {
        SelfExplainResponse response = selfExplainService.evaluate(user.getId(), lectureId, request);
        return ResponseEntity.ok(ApiResponse.ok("자기 설명 평가가 완료되었습니다.", response));
    }

    @Operation(summary = "특정 강의의 자기 설명 기록", description = "현재 사용자가 해당 강의에서 작성한 자기 설명 기록을 최신순으로 반환합니다.")
    @GetMapping("/api/lectures/{lectureId}/self-explain/history")
    public ResponseEntity<ApiResponse<List<SelfExplainHistoryItem>>> getLectureHistory(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user) {
        List<SelfExplainHistoryItem> history = selfExplainService.getHistoryByLecture(user.getId(), lectureId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @Operation(summary = "내 자기 설명 전체 기록 (회고용)", description = "사용자가 작성한 모든 자기 설명 기록을 최신순으로 반환합니다.")
    @GetMapping("/api/me/self-explain/history")
    public ResponseEntity<ApiResponse<List<SelfExplainHistoryItem>>> getMyHistory(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<SelfExplainHistoryItem> history = selfExplainService.getAllHistory(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
