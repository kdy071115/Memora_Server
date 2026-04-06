package com.kit.memora_server.domain.feedback.controller;

import com.kit.memora_server.domain.feedback.dto.FeedbackRequest;
import com.kit.memora_server.domain.feedback.dto.FeedbackResponse;
import com.kit.memora_server.domain.feedback.service.FeedbackService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Feedback", description = "교직자 피드백 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "피드백 작성 (교직자)")
    @PostMapping("/api/courses/{courseId}/students/{studentId}/feedback")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<FeedbackResponse>> create(
            @PathVariable Long courseId,
            @PathVariable Long studentId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody FeedbackRequest request) {
        FeedbackResponse response = feedbackService.create(courseId, studentId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("피드백이 작성되었습니다.", response));
    }

    @Operation(summary = "학생별 피드백 조회 (교직자)")
    @GetMapping("/api/courses/{courseId}/students/{studentId}/feedback")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getByCourseAndStudent(
            @PathVariable Long courseId,
            @PathVariable Long studentId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                feedbackService.getByCourseAndStudent(courseId, studentId, user.getId())
        ));
    }

    @Operation(summary = "내가 받은 피드백")
    @GetMapping("/api/feedback/me")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getMyFeedback(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(feedbackService.getMyFeedback(user.getId())));
    }

    @Operation(summary = "피드백 읽음 처리")
    @PatchMapping("/api/feedback/{feedbackId}/read")
    public ResponseEntity<ApiResponse<FeedbackResponse>> markAsRead(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                "읽음 처리되었습니다.",
                feedbackService.markAsRead(feedbackId, user.getId())
        ));
    }
}
