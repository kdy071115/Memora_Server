package com.kit.memora_server.domain.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CourseRequest {

    @NotBlank(message = "강의명은 필수입니다.")
    private String title;

    private String description;
}
