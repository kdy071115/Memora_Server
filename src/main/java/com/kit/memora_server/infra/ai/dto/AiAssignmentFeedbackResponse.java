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
public class AiAssignmentFeedbackResponse {
    private Integer overallScore;
    private String grade;
    private String summary;
    private List<String> strengths;
    private List<String> improvements;
    private List<String> missingPoints;
    private List<String> suggestions;
    private String instructorDraft;
}
