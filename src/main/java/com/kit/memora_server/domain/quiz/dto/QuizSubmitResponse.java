package com.kit.memora_server.domain.quiz.dto;

import com.kit.memora_server.domain.quiz.entity.QuizAttempt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QuizSubmitResponse {

    private Long attemptId;
    private Boolean isCorrect;
    private Integer score;
    private String correctAnswer;
    private String explanation;
    private String aiFeedback;
    private Integer timeSpent;

    public static QuizSubmitResponse from(QuizAttempt attempt) {
        return QuizSubmitResponse.builder()
                .attemptId(attempt.getId())
                .isCorrect(attempt.getIsCorrect())
                .score(attempt.getScore())
                .correctAnswer(attempt.getQuiz().getCorrectAnswer())
                .explanation(attempt.getQuiz().getExplanation())
                .aiFeedback(attempt.getAiFeedback())
                .timeSpent(attempt.getTimeSpent())
                .build();
    }
}
