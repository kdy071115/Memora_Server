package com.kit.memora_server.infra.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAudioTranscribeResponse {
    private String fullTranscript;
    private String summary;
    private List<Chapter> chapters;
    private Double durationSec;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Chapter {
        private String title;
        private Double startSec;
        private String summary;
    }
}
