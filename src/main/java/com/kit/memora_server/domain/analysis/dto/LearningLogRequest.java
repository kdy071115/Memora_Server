package com.kit.memora_server.domain.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "학습 활동 heartbeat 요청")
public class LearningLogRequest {

    @NotNull
    @Min(1)
    @Max(600)
    @Schema(description = "이번 heartbeat 가 누적한 활동 시간 (초). 한 번 호출 당 최대 10분.",
            example = "30")
    private Integer duration;

    @Schema(description = "활동 종류 (LEARN / QUIZ / QA)", example = "LEARN")
    private String activityType;
}
