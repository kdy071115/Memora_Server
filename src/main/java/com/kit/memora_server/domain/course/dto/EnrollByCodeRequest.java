package com.kit.memora_server.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "초대 코드 기반 수강 등록 요청")
public class EnrollByCodeRequest {

    @NotBlank(message = "초대 코드는 필수입니다.")
    @Schema(description = "강의 초대 코드 (8자리 대문자+숫자)", example = "A7KQ3M2P", requiredMode = Schema.RequiredMode.REQUIRED)
    private String inviteCode;
}
