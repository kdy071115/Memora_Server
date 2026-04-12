package com.kit.memora_server.domain.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "팀 초대 요청")
public class TeamInvitationRequest {

    @NotNull
    @Schema(description = "초대할 사용자 ID", example = "5")
    private Long inviteeId;
}
