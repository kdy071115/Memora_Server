package com.kit.memora_server.infra.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiDocumentCallbackRequest {

    private Long documentId;
    private String status;
    private String summary;
    private List<ChunkData> chunks;

    @Getter
    @NoArgsConstructor
    public static class ChunkData {
        private int chunkIndex;
        private String content;
        private Integer pageNumber;
        private Integer tokenCount;
        private String embeddingId;
    }
}
