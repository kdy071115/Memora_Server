package com.kit.memora_server.domain.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "약점 개념 (정답률이 낮고 시도 수가 일정 이상인 컨셉)")
public class WeakConceptDto {

    @Schema(description = "컨셉 태그", example = "미적분")
    private String concept;

    @Schema(description = "정답률 (0.0 ~ 1.0)", example = "0.42")
    private Double correctRate;

    @Schema(description = "시도 횟수", example = "12")
    private long attemptCount;
}
