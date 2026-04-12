package com.kit.memora_server.domain.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QuizSubmitRequest {

    @NotNull
    private String userAnswer;

    private Integer timeSpent;
}
