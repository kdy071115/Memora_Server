package com.kit.memora_server.domain.quiz.dto;

import com.kit.memora_server.domain.quiz.entity.QuizAttempt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class QuizAttemptResponse {

    private Long attemptId;
    private Long quizId;
    private String question;
    private String quizType;
    private String userAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private Integer score;
    private String conceptTag;
    private LocalDateTime attemptedAt;

    public static QuizAttemptResponse from(QuizAttempt attempt) {
        return QuizAttemptResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .question(attempt.getQuiz().getQuestion())
                .quizType(attempt.getQuiz().getQuizType())
                .userAnswer(attempt.getUserAnswer())
                .correctAnswer(attempt.getQuiz().getCorrectAnswer())
                .isCorrect(attempt.getIsCorrect())
                .score(attempt.getScore())
                .conceptTag(attempt.getQuiz().getConceptTag())
                .attemptedAt(attempt.getAttemptedAt())
                .build();
    }
}
