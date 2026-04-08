package com.kit.memora_server.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "강의 수강생 (간단 정보) — 팀 초대 등에 사용")
public class CourseMemberDto {
    private Long userId;
    private String name;
    private String email;
}
