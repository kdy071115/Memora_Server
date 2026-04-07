package com.kit.memora_server.domain.analysis.controller;

import com.kit.memora_server.domain.analysis.dto.LearningLogRequest;
import com.kit.memora_server.domain.analysis.service.LearningLogService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "LearningLog", description = "학습 활동 heartbeat API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LearningLogController {

    private final LearningLogService learningLogService;

    @Operation(summary = "학습 활동 heartbeat 기록",
            description = "학습/QA/퀴즈 화면이 열려 있을 때 일정 주기로 호출. duration(초) 만큼 누적됩니다.")
    @PostMapping("/api/lectures/{lectureId}/learning-logs")
    public ResponseEntity<ApiResponse<Void>> record(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody LearningLogRequest request) {
        learningLogService.recordHeartbeat(user.getId(), lectureId, request);
        return ResponseEntity.ok(ApiResponse.ok("학습 활동이 기록되었습니다."));
    }
}
