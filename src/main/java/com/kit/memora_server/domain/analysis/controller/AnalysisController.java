package com.kit.memora_server.domain.analysis.controller;

import com.kit.memora_server.domain.analysis.dto.CourseOverviewResponse;
import com.kit.memora_server.domain.analysis.dto.CourseStudentDetailResponse;
import com.kit.memora_server.domain.analysis.dto.CourseStudentSummary;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Operation(summary = "강의 분석 대시보드 (교직자)")
    @GetMapping("/api/analysis/courses/{courseId}/overview")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<CourseOverviewResponse>> getCourseOverview(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                analysisService.getCourseOverview(courseId, user.getId())
        ));
    }

    @Operation(summary = "강의 수강생 목록 (교직자)")
    @GetMapping("/api/analysis/courses/{courseId}/students")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<CourseStudentSummary>>> getCourseStudents(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                analysisService.getCourseStudents(courseId, user.getId())
        ));
    }

    @Operation(summary = "수강생 개별 드릴다운 분석 (교직자)")
    @GetMapping("/api/analysis/courses/{courseId}/students/{userId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<CourseStudentDetailResponse>> getCourseStudentDetail(
            @PathVariable Long courseId,
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                analysisService.getCourseStudentDetail(courseId, userId, user.getId())
        ));
    }
}
