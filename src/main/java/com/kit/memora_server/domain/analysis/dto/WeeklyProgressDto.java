package com.kit.memora_server.domain.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "주간 진행도 (ISO 주 단위)")
public class WeeklyProgressDto {

    @Schema(description = "ISO 주 (YYYY-WNN)", example = "2026-W14")
    private String week;

    @Schema(description = "학습 시간 (초)", example = "3600")
    private long studyTime;

    @Schema(description = "해당 주 평균 퀴즈 점수 (0~100)", example = "78", nullable = true)
    private Integer quizScore;
}
