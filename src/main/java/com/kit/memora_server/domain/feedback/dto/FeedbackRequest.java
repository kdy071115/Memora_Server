package com.kit.memora_server.domain.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "교직자 피드백 작성 요청")
public class FeedbackRequest {

    @NotBlank(message = "피드백 내용은 필수입니다.")
    @Schema(description = "피드백 내용", example = "최근 퀴즈 성적이 많이 올랐어요. 계속 이 페이스로 가세요!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
