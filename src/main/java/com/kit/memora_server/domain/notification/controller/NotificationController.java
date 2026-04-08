package com.kit.memora_server.domain.notification.controller;

import com.kit.memora_server.domain.notification.dto.NotificationCountResponse;
import com.kit.memora_server.domain.notification.service.NotificationService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "헤더 종 아이콘 알림 카운트")
@RestController
@RequestMapping("/api/me/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 카운트 (팀 초대 + 미확인 피드백)")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationCountResponse>> getCounts(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getCounts(user.getId())));
    }

    @Operation(summary = "피드백 알림을 모두 확인 처리 — 학생이 종을 클릭하면 호출")
    @PostMapping("/feedback/mark-seen")
    public ResponseEntity<ApiResponse<Void>> markFeedbackSeen(
            @AuthenticationPrincipal CustomUserDetails user) {
        notificationService.markFeedbackSeen(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("피드백 알림을 확인 처리했습니다."));
    }
}
