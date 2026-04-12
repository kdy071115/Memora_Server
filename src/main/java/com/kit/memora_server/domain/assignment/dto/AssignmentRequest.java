package com.kit.memora_server.domain.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "과제 생성/수정 요청")
public class AssignmentRequest {

    @NotBlank
    @Size(max = 200)
    @Schema(description = "과제 제목", example = "5장 보고서")
    private String title;

    @NotBlank
    @Schema(description = "과제 설명 (markdown 가능)", example = "PDF로 5페이지 이상 작성하여 제출")
    private String description;

    @Schema(description = "마감일 (옵션)", example = "2026-04-20T23:59:59")
    private LocalDateTime dueDate;

    @Schema(description = "팀 단위 제출 허용 여부", example = "true")
    private Boolean allowTeamSubmission;
}
