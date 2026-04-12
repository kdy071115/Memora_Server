package com.kit.memora_server.domain.dailymission.controller;

import com.kit.memora_server.domain.dailymission.dto.DailyMissionResponse;
import com.kit.memora_server.domain.dailymission.service.DailyMissionService;
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

@Tag(name = "DailyMission", description = "학생 개인화 데일리 학습 미션")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DailyMissionController {

    private final DailyMissionService dailyMissionService;

    @Operation(summary = "내 오늘의 학습 미션 (학생용)")
    @GetMapping("/api/me/daily-missions")
    public ResponseEntity<ApiResponse<DailyMissionResponse>> getMyDailyMissions(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(dailyMissionService.getMyDailyMissions(user.getId())));
    }
}
