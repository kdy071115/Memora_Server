package com.kit.memora_server.domain.assignment.dto;

import com.kit.memora_server.domain.assignment.entity.SubmissionVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "제출물 작성/수정 요청")
public class SubmissionRequest {

    @NotBlank
    @Schema(description = "제출 본문 (markdown 가능)")
    private String content;

    @Schema(description = "공개 범위", example = "PRIVATE", allowableValues = {"PUBLIC", "PRIVATE"})
    private SubmissionVisibility visibility;

    @Schema(description = "팀 ID — 팀 제출인 경우. 없으면 개인 제출", example = "3")
    private Long teamId;
}
