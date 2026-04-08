package com.kit.memora_server.domain.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@com.fasterxml.jackson.databind.annotation.JsonDeserialize
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@Schema(description = "강사용 AI 과제 피드백 초안")
public class AiFeedbackResponse {

    @Schema(description = "PENDING / READY / FAILED — 캐시 polling 시 상태 확인용")
    private String status;

    @Schema(description = "FAILED 일 때 에러 메시지")
    private String errorMessage;

    @Schema(description = "0~100 종합 점수")
    private Integer overallScore;

    @Schema(description = "EXCELLENT / GOOD / AVERAGE / NEEDS_WORK")
    private String grade;

    @Schema(description = "1-2 줄 종합 평가")
    private String summary;

    @Schema(description = "잘한 점")
    private List<String> strengths;

    @Schema(description = "보완 포인트")
    private List<String> improvements;

    @Schema(description = "과제 안내에 있었지만 빠뜨린 항목")
    private List<String> missingPoints;

    @Schema(description = "다음 단계 학습 제안")
    private List<String> suggestions;

    @Schema(description = "강사가 그대로 댓글로 사용할 수 있는 자연어 피드백 초안")
    private String instructorDraft;
}
