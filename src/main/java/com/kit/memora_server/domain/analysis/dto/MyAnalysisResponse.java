package com.kit.memora_server.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

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

    /**
     * 프론트 레이더 차트용 6개 역량 점수 (0~150 스케일)
     * key: "개념 이해력" / "수학적 사고" / "비판적 추론" / "암기력" / "응용력" / "문제 해결"
     */
    private Map<String, Integer> competencies;

    /** 최대 성장 지표 문구 (예: "개념 이해력이 지난주 대비 30% 상승") */
    private String maxGrowthIndicator;
}
