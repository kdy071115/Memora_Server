package com.kit.memora_server.domain.atrisk.controller;

import com.kit.memora_server.domain.atrisk.dto.AtRiskStudentItem;
import com.kit.memora_server.domain.atrisk.dto.CareMessageResponse;
import com.kit.memora_server.domain.atrisk.service.AtRiskService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AtRisk", description = "강사용 — 위험 학생 진단 + 케어 메시지")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('INSTRUCTOR')")
public class AtRiskController {

    private final AtRiskService atRiskService;

    @Operation(summary = "강의의 케어가 필요한 학생 명단 (위험 점수 내림차순)")
    @GetMapping("/api/courses/{courseId}/at-risk-students")
    public ResponseEntity<ApiResponse<List<AtRiskStudentItem>>> getAtRisk(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(atRiskService.getAtRiskByCourse(user.getId(), courseId, limit)));
    }

    @Operation(summary = "특정 위험 학생에 대한 AI 케어 메시지 초안 생성")
    @PostMapping("/api/courses/{courseId}/at-risk-students/{studentId}/care-message")
    public ResponseEntity<ApiResponse<CareMessageResponse>> generateCareMessage(
            @PathVariable Long courseId,
            @PathVariable Long studentId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                "케어 메시지 초안을 생성했습니다.",
                atRiskService.generateCareMessage(user.getId(), courseId, studentId)
        ));
    }
}
