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
public class AiCareMessageRequest {
    private String studentName;
    private String courseTitle;
    private String instructorName;
    private List<String> riskReasons;
    private List<String> weakConcepts;
    private Integer daysSinceLastActive;
    private Integer averageScore;
}
