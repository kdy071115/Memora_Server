package com.kit.memora_server.domain.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "교직자용 강의 분석 대시보드 응답")
public class CourseOverviewResponse {

    @Schema(description = "강의 ID", example = "1")
    private Long courseId;

    @Schema(description = "강의명", example = "인공지능 개론")
    private String courseTitle;

    @Schema(description = "총 수강생 수", example = "35")
    private long totalStudents;

    @Schema(description = "최근 7일 이내 활동 학생 수", example = "28")
    private long activeStudents;

    @Schema(description = "수강생 overallScore 평균", example = "72")
    private Integer averageScore;

    @Schema(description = "학급 평균 정답률 (0.0 ~ 1.0)", example = "0.68")
    private double averageCorrectRate;

    @Schema(description = "1인당 평균 학습 시간 (초)", example = "3600")
    private long averageStudyTime;

    @Schema(description = "학급 전체 퀴즈 시도 수", example = "420")
    private long totalQuizAttempts;

    @Schema(description = "학급 집계 상위 약점 개념 (최대 5개)")
    private List<WeakConceptDto> topWeakConcepts;

    @Schema(description = "학급 평균 6개 역량 (0~150 스케일)",
            example = "{\"개념 이해력\": 72, \"수학적 사고\": 70, \"비판적 추론\": 68, \"암기력\": 74, \"응용력\": 65, \"문제 해결\": 69}")
    private Map<String, Integer> competencies;

    @Schema(description = "학급 평균 최근 4주 진행도")
    private List<WeeklyProgressDto> weeklyProgress;

    @Schema(description = "학생 분포")
    private StudentDistributionDto studentDistribution;
}
