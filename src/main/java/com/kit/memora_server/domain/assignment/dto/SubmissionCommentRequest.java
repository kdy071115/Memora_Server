package com.kit.memora_server.domain.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "제출물 댓글 작성 요청")
public class SubmissionCommentRequest {

    @NotBlank
    @Size(min = 1, max = 4000)
    @Schema(description = "댓글 본문")
    private String content;
}
