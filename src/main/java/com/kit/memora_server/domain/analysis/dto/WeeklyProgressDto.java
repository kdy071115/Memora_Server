package com.kit.memora_server.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WeeklyProgressDto {
    private String week;
    private long studyTime;
    private Integer quizScore;
}
