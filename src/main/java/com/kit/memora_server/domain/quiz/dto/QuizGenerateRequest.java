package com.kit.memora_server.domain.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuizGenerateRequest {

    @Min(1)
    @Max(20)
    private Integer count = 5;

    private List<String> types = List.of("MULTIPLE_CHOICE");

    private String difficulty = "MEDIUM";

    private List<String> conceptTags = List.of();
}
