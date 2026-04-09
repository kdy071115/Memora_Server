package com.kit.memora_server.domain.atrisk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "강사용 — 케어가 필요한 학생 1명의 위험 진단 결과")
public class AtRiskStudentItem {

    private Long userId;
    private String name;
    private String email;

    /** 0~100. 높을수록 위험. */
    private int riskScore;

    /** "HIGH" / "MEDIUM" / "LOW" */
    private String riskLevel;

    /** 사람이 읽을 수 있는 위험 사유 (UI 칩 + AI 케어 메시지 컨텍스트로 사용) */
    private List<String> reasons;

    private Integer averageScore;
    private Integer correctRatePercent;
    private Long totalStudyTimeSeconds;
    private LocalDateTime lastActiveAt;
    private Integer daysSinceLastActive;
    private long pendingAssignments;
    private List<String> weakConcepts;
}
