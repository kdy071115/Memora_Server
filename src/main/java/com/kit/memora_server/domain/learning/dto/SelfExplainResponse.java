package com.kit.memora_server.domain.learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "자기 설명 평가 결과")
public class SelfExplainResponse {

    @Schema(description = "0~100 종합 점수", example = "78")
    private Integer overallScore;

    @Schema(description = "EXCELLENT / GOOD / NEEDS_WORK", example = "GOOD")
    private String grade;

    @Schema(description = "잘 이해한 부분")
    private List<String> strengths;

    @Schema(description = "빠뜨린 핵심 개념")
    private List<String> missingConcepts;

    @Schema(description = "오개념 — 잘못 이해한 부분")
    private List<String> misconceptions;

    @Schema(description = "전체 평가 피드백 (자유 서술)")
    private String feedback;

    @Schema(description = "다음에 시도할 학습 단계 제안")
    private List<String> suggestedNextSteps;
}
