package com.kit.memora_server.domain.notice.controller;

import com.kit.memora_server.domain.notice.dto.NoticeRequest;
import com.kit.memora_server.domain.notice.dto.NoticeResponse;
import com.kit.memora_server.domain.notice.service.NoticeService;
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

@Tag(name = "Notice", description = "강의 공지사항 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지사항 작성 (교직자)")
    @PostMapping("/api/courses/{courseId}/notices")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<NoticeResponse>> create(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody NoticeRequest request) {
        NoticeResponse response = noticeService.create(courseId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("공지사항이 작성되었습니다.", response));
    }

    @Operation(summary = "공지사항 목록")
    @GetMapping("/api/courses/{courseId}/notices")
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> getByCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.getByCourse(courseId, user.getId())));
    }

    @Operation(summary = "공지사항 수정 (교직자)")
    @PutMapping("/api/notices/{noticeId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<NoticeResponse>> update(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody NoticeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "공지사항이 수정되었습니다.",
                noticeService.update(noticeId, user.getId(), request)
        ));
    }

    @Operation(summary = "공지사항 삭제 (교직자)")
    @DeleteMapping("/api/notices/{noticeId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal CustomUserDetails user) {
        noticeService.delete(noticeId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("공지사항이 삭제되었습니다."));
    }
}
