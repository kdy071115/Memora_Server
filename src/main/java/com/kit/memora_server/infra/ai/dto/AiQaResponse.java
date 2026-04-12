package com.kit.memora_server.infra.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiQaResponse {

    private String answer;
    private List<AiSource> sources;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiSource {
        private Long documentId;
        private String documentName;
        private Integer pageNumber;
        private String preview;
    }
}
