package com.kit.memora_server.domain.learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "자기 설명 평가 기록 한 건")
public class SelfExplainHistoryItem {

    private Long id;
    private Long lectureId;
    private String lectureTitle;
    private Long courseId;
    private String courseTitle;

    private Integer overallScore;
    private String grade;

    private String focusTopic;
    private String explanation;

    private List<String> strengths;
    private List<String> missingConcepts;
    private List<String> misconceptions;

    private LocalDateTime createdAt;
}
