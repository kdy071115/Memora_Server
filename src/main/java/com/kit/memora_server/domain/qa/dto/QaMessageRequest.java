package com.kit.memora_server.domain.qa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QaMessageRequest {

    @NotBlank(message = "질문 내용은 비어 있을 수 없습니다.")
    private String content;

    private String difficulty = "MEDIUM";

    /** "NORMAL" (기본) 또는 "SOCRATIC" (소크라테스 역질문 모드) */
    private String mode = "NORMAL";
}
