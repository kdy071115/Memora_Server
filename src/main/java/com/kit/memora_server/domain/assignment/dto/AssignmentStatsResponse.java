package com.kit.memora_server.domain.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "강사용 과제 제출 통계")
public class AssignmentStatsResponse {

    @Schema(description = "전체 수강생 수", example = "30")
    private long totalStudents;

    @Schema(description = "한 번이라도 제출한 학생 수 (개인 + 팀 제출자 포함)", example = "23")
    private long submittedStudents;

    @Schema(description = "제출률 (0~100 정수)", example = "76")
    private int submissionRate;

    @Schema(description = "미제출 학생 명단")
    private List<MissingStudent> missing;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "미제출 학생 한 명")
    public static class MissingStudent {
        private Long userId;
        private String name;
        private String email;
    }
}
