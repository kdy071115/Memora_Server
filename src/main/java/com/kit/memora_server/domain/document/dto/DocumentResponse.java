package com.kit.memora_server.domain.document.dto;

import com.kit.memora_server.domain.document.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private String processingStatus;
    private Integer chunkCount;
    private LocalDateTime createdAt;

    public static DocumentResponse from(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .originalName(document.getOriginalName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .processingStatus(document.getProcessingStatus())
                .chunkCount(document.getChunkCount())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
