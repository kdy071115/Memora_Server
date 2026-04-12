package com.kit.memora_server.infra.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisRequest {
    private Double correctRate;
    private List<String> weakConcepts;
    private List<String> questionPatterns;
    private String studyTimeTrend;
}
