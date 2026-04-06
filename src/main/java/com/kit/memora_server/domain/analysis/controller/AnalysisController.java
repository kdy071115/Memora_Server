package com.kit.memora_server.domain.analysis.controller;

import com.kit.memora_server.domain.analysis.dto.MyAnalysisResponse;
import com.kit.memora_server.domain.analysis.service.AnalysisService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analysis", description = "학습 분석 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "내 종합 학습 분석")
    @GetMapping("/api/analysis/me")
    public ResponseEntity<ApiResponse<MyAnalysisResponse>> getMyAnalysis(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getMyAnalysis(user.getId())));
    }
}
