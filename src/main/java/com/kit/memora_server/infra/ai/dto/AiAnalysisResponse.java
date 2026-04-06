package com.kit.memora_server.infra.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysisResponse {
    private String diagnosis;
    private String weakConceptAnalysis;
    private List<String> recommendations;
    private String motivation;
    /** 프론트 레이더 차트용 6개 역량 점수 (0~150) */
    private Map<String, Integer> competencies;
    /** 최대 성장 지표 문구 */
    private String maxGrowthIndicator;
}
