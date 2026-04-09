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
public class AiDailyMissionResponse {
    private String summary;
    private List<MissionItem> missions;
    private String motivation;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MissionItem {
        private String type;
        private String title;
        private String description;
        private Integer estimatedMinutes;
        private String why;
    }
}
