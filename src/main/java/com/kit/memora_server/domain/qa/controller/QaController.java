package com.kit.memora_server.domain.qa.controller;

import com.kit.memora_server.domain.qa.dto.QaMessageRequest;
import com.kit.memora_server.domain.qa.dto.QaMessageResponse;
import com.kit.memora_server.domain.qa.dto.QaSessionResponse;
import com.kit.memora_server.domain.qa.dto.QaSessionSummaryResponse;
import com.kit.memora_server.domain.qa.service.QaService;
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

@Tag(name = "QA", description = "AI 질의응답 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class QaController {

    private final QaService qaService;

    @Operation(summary = "QA 세션 생성")
    @PostMapping("/api/lectures/{lectureId}/qa/sessions")
    public ResponseEntity<ApiResponse<QaSessionResponse>> createSession(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user) {
        QaSessionResponse response = qaService.createSession(user.getId(), lectureId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("QA 세션이 생성되었습니다.", response));
    }

    @Operation(summary = "내 QA 세션 목록")
    @GetMapping("/api/qa/sessions")
    public ResponseEntity<ApiResponse<List<QaSessionSummaryResponse>>> getMySessions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Long lectureId) {
        return ResponseEntity.ok(ApiResponse.ok(qaService.getMySessions(user.getId(), lectureId)));
    }

    @Operation(summary = "세션 상세 (대화 내역)")
    @GetMapping("/api/qa/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<QaSessionResponse>> getSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(qaService.getSession(user.getId(), sessionId)));
    }

    @Operation(summary = "질문 전송 (AI 응답)")
    @PostMapping("/api/qa/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<QaMessageResponse>> sendMessage(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody QaMessageRequest request) {
        QaMessageResponse response = qaService.sendMessage(user.getId(), sessionId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
