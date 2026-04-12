package com.kit.memora_server.domain.quiz.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kit.memora_server.domain.quiz.entity.Quiz;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 강사용 퀴즈 상세 응답. 학생용 {@link QuizResponse}와 달리 정답/해설을 포함합니다.
 */
@Getter
@Builder
@AllArgsConstructor
public class QuizDetailResponse {

    private Long id;
    private String question;
    private String quizType;
    private List<String> options;
    private String correctAnswer;
    private String explanation;
    private String difficulty;
    private String conceptTag;

    public static QuizDetailResponse from(Quiz quiz, ObjectMapper objectMapper) {
        List<String> opts = null;
        if (quiz.getOptions() != null && !quiz.getOptions().isBlank()) {
            try {
                opts = objectMapper.readValue(
                        quiz.getOptions(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                );
            } catch (JsonProcessingException ignored) {
            }
        }
        return QuizDetailResponse.builder()
                .id(quiz.getId())
                .question(quiz.getQuestion())
                .quizType(quiz.getQuizType())
                .options(opts)
                .correctAnswer(quiz.getCorrectAnswer())
                .explanation(quiz.getExplanation())
                .difficulty(quiz.getDifficulty())
                .conceptTag(quiz.getConceptTag())
                .build();
    }
}
