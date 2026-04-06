package com.kit.memora_server.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MyAnalysisResponse {

    private Integer overallScore;
    private long totalStudyTime;
    private long totalQuizAttempts;
    private double overallCorrectRate;
    private long enrolledCourses;
    private List<WeakConceptDto> weakConcepts;
    private String diagnosis;
    private List<String> recommendations;
    private String motivation;
    private List<WeeklyProgressDto> weeklyProgress;
}
