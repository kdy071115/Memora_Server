package com.kit.memora_server.infra.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiQaRequest {

    private Long lectureId;
    private String question;
    private String difficulty;
    /** "NORMAL" (기본) 또는 "SOCRATIC" (역질문 모드) */
    @Builder.Default
    private String mode = "NORMAL";
    private List<HistoryMessage> history;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryMessage {
        private String role;
        private String content;
    }
}
