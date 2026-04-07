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
public class AiSelfExplainResponse {
    private Integer overallScore;
    private String grade;
    private List<String> strengths;
    private List<String> missingConcepts;
    private List<String> misconceptions;
    private String feedback;
    private List<String> suggestedNextSteps;
}
