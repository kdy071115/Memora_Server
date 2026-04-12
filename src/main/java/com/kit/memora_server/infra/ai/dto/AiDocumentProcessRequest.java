package com.kit.memora_server.infra.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AiDocumentProcessRequest {

    private Long documentId;
    private Long lectureId;
    private String storedPath;
    private String callbackUrl;
}
