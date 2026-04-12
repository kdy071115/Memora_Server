package com.kit.memora_server.domain.document.controller;

import com.kit.memora_server.domain.document.dto.DocumentResponse;
import com.kit.memora_server.domain.document.dto.DocumentSummaryResponse;
import com.kit.memora_server.domain.document.service.DocumentService;
import com.kit.memora_server.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Document", description = "강의자료 API")
@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "자료 업로드")
    @PostMapping(value = "/api/lectures/{lectureId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @PathVariable Long lectureId,
            @RequestParam("file") MultipartFile file) {
        DocumentResponse response = documentService.upload(lectureId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok("업로드 완료. 문서 처리가 시작되었습니다.", response));
    }

    @Operation(summary = "자료 목록")
    @GetMapping("/api/lectures/{lectureId}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getByLecture(@PathVariable Long lectureId) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getByLecture(lectureId)));
    }

    @Operation(summary = "문서 요약 조회")
    @GetMapping("/api/documents/{documentId}/summary")
    public ResponseEntity<ApiResponse<DocumentSummaryResponse>> getSummary(@PathVariable Long documentId) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getSummary(documentId)));
    }
}
