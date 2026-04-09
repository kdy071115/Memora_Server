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
public class AiDailyMissionRequest {
    private String studentName;
    private List<String> weakConcepts;
    private List<String> pendingAssignments;
    private List<String> upcomingLectureTitles;
    private Integer averageScore;
    private Integer lastSelfExplainScore;
    private Integer daysSinceLastStudy;
}
