package com.kit.memora_server.domain.lecture.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LectureRequest {

    @NotBlank(message = "차시 제목은 필수입니다.")
    private String title;

    private String description;
}
