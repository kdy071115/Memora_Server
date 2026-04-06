package com.kit.memora_server.domain.quiz.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kit.memora_server.domain.quiz.entity.Quiz;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QuizResponse {

    private Long id;
    private String question;
    private String quizType;
    private List<String> options;
    private String difficulty;
    private String conceptTag;

    public static QuizResponse from(Quiz quiz, ObjectMapper objectMapper) {
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
        return QuizResponse.builder()
                .id(quiz.getId())
                .question(quiz.getQuestion())
                .quizType(quiz.getQuizType())
                .options(opts)
                .difficulty(quiz.getDifficulty())
                .conceptTag(quiz.getConceptTag())
                .build();
    }
}
