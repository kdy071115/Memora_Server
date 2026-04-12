package com.kit.memora_server.domain.dailymission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "학생용 — 오늘의 학습 미션")
public class DailyMissionResponse {

    @Schema(description = "오늘의 학습 한 줄 요약")
    private String summary;

    @Schema(description = "짧은 응원 메시지")
    private String motivation;

    @Schema(description = "추천 미션 (3-5개)")
    private List<MissionItem> missions;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MissionItem {
        @Schema(description = "READ / QUIZ / SELF_EXPLAIN / SUBMIT / REVIEW")
        private String type;
        private String title;
        private String description;
        private Integer estimatedMinutes;
        private String why;
    }
}
