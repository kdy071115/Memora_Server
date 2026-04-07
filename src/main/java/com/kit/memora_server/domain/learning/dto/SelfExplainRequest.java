package com.kit.memora_server.domain.learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "자기 설명 평가 요청")
public class SelfExplainRequest {

    @NotBlank
    @Size(min = 20, max = 4000, message = "설명은 20자 이상 4000자 이하로 작성해주세요.")
    @Schema(description = "학생이 본인 말로 작성한 설명",
            example = "광합성은 식물이 빛을 이용해 ...")
    private String explanation;

    @Schema(description = "특정 주제에 한해 설명한 경우 (옵션)", example = "광합성의 명반응")
    private String focusTopic;
}
