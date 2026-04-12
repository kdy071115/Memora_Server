package com.kit.memora_server.domain.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "학생 분포 (overallScore 기준 구간별 학생 수)")
public class StudentDistributionDto {

    @Schema(description = "우수 (85점 이상)", example = "8")
    private long excellent;

    @Schema(description = "양호 (70~84점)", example = "15")
    private long good;

    @Schema(description = "보통 (50~69점)", example = "9")
    private long average;

    @Schema(description = "도움 필요 (50점 미만 또는 최근 7일 미접속)", example = "3")
    private long needsHelp;
}
