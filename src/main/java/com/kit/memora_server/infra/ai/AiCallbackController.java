package com.kit.memora_server.infra.ai;

import com.kit.memora_server.domain.document.service.DocumentService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.infra.ai.dto.AiDocumentCallbackRequest;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Hidden
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class AiCallbackController {

    private final DocumentService documentService;

    @PostMapping("/documents/{documentId}/callback")
    public ResponseEntity<ApiResponse<Void>> documentCallback(
            @PathVariable Long documentId,
            @RequestBody AiDocumentCallbackRequest request) {
        documentService.handleCallback(documentId, request);
        return ResponseEntity.ok(ApiResponse.ok("콜백 처리 완료"));
    }
}
