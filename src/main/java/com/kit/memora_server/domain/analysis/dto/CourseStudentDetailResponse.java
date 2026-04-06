package com.kit.memora_server.domain.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "교직자용 수강생 개별 드릴다운 분석")
public class CourseStudentDetailResponse {

    @Schema(description = "학생 ID", example = "1")
    private Long userId;

    @Schema(description = "학생 이름", example = "김학생")
    private String userName;

    @Schema(description = "학생 이메일", example = "student1@kit.ac.kr")
    private String userEmail;

    @Schema(description = "강의 ID", example = "1")
    private Long courseId;

    @Schema(description = "강의명", example = "인공지능 개론")
    private String courseTitle;

    @Schema(description = "강의 범위 종합 점수", example = "78")
    private Integer overallScore;

    @Schema(description = "학습 시간 (초)", example = "4200")
    private long totalStudyTime;

    @Schema(description = "퀴즈 시도 수", example = "25")
    private long totalQuizAttempts;

    @Schema(description = "정답률 (0.0 ~ 1.0)", example = "0.72")
    private double overallCorrectRate;

    @Schema(description = "마지막 활동 시각", nullable = true, example = "2026-04-06T14:30:00")
    private LocalDateTime lastActiveAt;

    @Schema(description = "학습 상태", example = "GOOD", allowableValues = {"EXCELLENT", "GOOD", "AVERAGE", "NEEDS_HELP"})
    private String status;

    @Schema(description = "약점 개념 (상위 5개)")
    private List<WeakConceptDto> weakConcepts;

    @Schema(description = "최근 4주 진행도")
    private List<WeeklyProgressDto> weeklyProgress;

    @Schema(description = "6개 역량 (0~150 스케일)")
    private Map<String, Integer> competencies;
}
