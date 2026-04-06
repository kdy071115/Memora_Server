package com.kit.memora_server.domain.document.dto;

import com.kit.memora_server.domain.document.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DocumentSummaryResponse {

    private Long id;
    private String originalName;
    private String summary;

    public static DocumentSummaryResponse from(Document document) {
        return DocumentSummaryResponse.builder()
                .id(document.getId())
                .originalName(document.getOriginalName())
                .summary(document.getSummary())
                .build();
    }
}
