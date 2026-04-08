package com.kit.memora_server.domain.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "팀 생성/수정 요청")
public class TeamRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "팀 이름", example = "알파팀")
    private String name;

    @Size(max = 500)
    @Schema(description = "팀 설명 (옵션)")
    private String description;
}
