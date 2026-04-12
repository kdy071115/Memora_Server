package com.kit.memora_server.domain.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "학생 대시보드 상단 요약 카드 데이터")
public class StudentDashboardSummary {

    @Schema(description = "이번 주(월요일 00:00 부터 지금까지) 학습 시간 (초)", example = "5400")
    private long thisWeekStudyTime;

    @Schema(description = "누적 총 학습 시간 (초)", example = "180000")
    private long totalStudyTime;

    @Schema(description = "내 마감 임박(미래) 미제출 과제 수", example = "3")
    private long pendingAssignments;

    @Schema(description = "현재 평균 점수 (강의 전체 분석 점수)", example = "82")
    private int averageScore;

    @Schema(description = "강의 전체 정답률 0~100 (%)", example = "78")
    private int overallCorrectRate;
}
