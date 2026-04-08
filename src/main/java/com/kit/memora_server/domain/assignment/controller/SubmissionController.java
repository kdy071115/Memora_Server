package com.kit.memora_server.domain.assignment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kit.memora_server.domain.assignment.dto.SubmissionCommentRequest;
import com.kit.memora_server.domain.assignment.dto.SubmissionCommentResponse;
import com.kit.memora_server.domain.assignment.dto.SubmissionRequest;
import com.kit.memora_server.domain.assignment.dto.SubmissionResponse;
import com.kit.memora_server.domain.assignment.entity.Submission;
import com.kit.memora_server.domain.assignment.service.SubmissionService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Tag(name = "Submission", description = "과제 제출물 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "제출물 생성 (multipart)",
            description = "JSON 본문은 'request' 파트에 application/json 으로, 첨부 파일은 'file' 파트에 담아 multipart/form-data 로 전송합니다.")
    @PostMapping(value = "/api/assignments/{assignmentId}/submissions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SubmissionResponse>> create(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("request") String requestJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        SubmissionRequest req = parseRequest(requestJson);
        return ResponseEntity.ok(ApiResponse.ok("제출되었습니다.", submissionService.create(user.getId(), assignmentId, req, file)));
    }

    @Operation(summary = "과제의 (가시성 필터된) 제출물 목록")
    @GetMapping("/api/assignments/{assignmentId}/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> list(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.listVisible(user.getId(), assignmentId)));
    }

    @Operation(summary = "제출물 상세 (댓글 포함)")
    @GetMapping("/api/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<SubmissionResponse>> get(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.get(user.getId(), submissionId)));
    }

    @Operation(summary = "제출물 수정 (multipart)")
    @PutMapping(value = "/api/submissions/{submissionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SubmissionResponse>> update(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("request") String requestJson,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "removeAttachment", defaultValue = "false") boolean removeAttachment) {
        SubmissionRequest req = parseRequest(requestJson);
        return ResponseEntity.ok(ApiResponse.ok("수정되었습니다.", submissionService.update(user.getId(), submissionId, req, file, removeAttachment)));
    }

    @Operation(summary = "제출물 삭제")
    @DeleteMapping("/api/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        submissionService.delete(user.getId(), submissionId);
        return ResponseEntity.ok(ApiResponse.ok("삭제되었습니다."));
    }

    @Operation(summary = "제출물 첨부 파일 다운로드 (로컬 모드 전용)")
    @GetMapping("/api/submissions/{submissionId}/file")
    public ResponseEntity<Resource> download(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        Submission s = submissionService.rawForDownload(user.getId(), submissionId);
        if (s.getAttachmentPath() == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        try {
            // 로컬 모드에서는 절대경로. S3 모드는 별도 presigned URL 처리 권장 — MVP 는 로컬 위주.
            FileInputStream fis = new FileInputStream(s.getAttachmentPath());
            String fileName = s.getAttachmentName() != null ? s.getAttachmentName() : "attachment";
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            MediaType contentType = guessContentType(fileName);
            return ResponseEntity.ok()
                    // inline 으로 보내면 브라우저가 PDF/이미지 등을 새 탭에서 렌더 가능,
                    // 다운로드는 프론트에서 a[download] 로 강제하므로 inline 이 더 유연하다.
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded)
                    .contentType(contentType)
                    .body(new InputStreamResource(fis));
        } catch (IOException e) {
            log.error("첨부 파일 읽기 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 파일명 확장자로 Content-Type 추론. 알 수 없으면 octet-stream.
     * 프론트의 미리보기 가능 여부 판단도 이 값에 의존하므로 가급적 정확하게 채운다.
     */
    private MediaType guessContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return MediaType.parseMediaType("text/plain;charset=UTF-8");
        if (lower.endsWith(".csv")) return MediaType.parseMediaType("text/csv;charset=UTF-8");
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return MediaType.parseMediaType("text/html;charset=UTF-8");
        if (lower.endsWith(".json")) return MediaType.APPLICATION_JSON;
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    // ── Comments ──

    @Operation(summary = "제출물에 댓글 작성 (강사 또는 본인)")
    @PostMapping("/api/submissions/{submissionId}/comments")
    public ResponseEntity<ApiResponse<SubmissionCommentResponse>> addComment(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody SubmissionCommentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("댓글이 작성되었습니다.", submissionService.addComment(user.getId(), submissionId, request)));
    }

    @Operation(summary = "제출물 댓글 삭제")
    @DeleteMapping("/api/submissions/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user) {
        submissionService.deleteComment(user.getId(), commentId);
        return ResponseEntity.ok(ApiResponse.ok("댓글이 삭제되었습니다."));
    }

    private SubmissionRequest parseRequest(String json) {
        try {
            return objectMapper.readValue(json, SubmissionRequest.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
