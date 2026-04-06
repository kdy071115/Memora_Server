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
public class AiQuizGenerateResponse {

    private List<GeneratedQuiz> quizzes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeneratedQuiz {
        private String question;
        private String quizType;
        private List<String> options;
        private String correctAnswer;
        private String explanation;
        private String conceptTag;
        private String difficulty;
    }
}
