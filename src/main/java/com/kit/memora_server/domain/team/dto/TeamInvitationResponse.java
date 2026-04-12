package com.kit.memora_server.domain.team.dto;

import com.kit.memora_server.domain.team.entity.InvitationStatus;
import com.kit.memora_server.domain.team.entity.TeamInvitation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "팀 초대장")
public class TeamInvitationResponse {

    private Long id;
    private Long teamId;
    private String teamName;
    private Long courseId;
    private String courseTitle;
    private Long inviterId;
    private String inviterName;
    private Long inviteeId;
    private String inviteeName;
    private InvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public static TeamInvitationResponse from(TeamInvitation inv) {
        return TeamInvitationResponse.builder()
                .id(inv.getId())
                .teamId(inv.getTeam().getId())
                .teamName(inv.getTeam().getName())
                .courseId(inv.getTeam().getCourse().getId())
                .courseTitle(inv.getTeam().getCourse().getTitle())
                .inviterId(inv.getInviter().getId())
                .inviterName(inv.getInviter().getName())
                .inviteeId(inv.getInvitee().getId())
                .inviteeName(inv.getInvitee().getName())
                .status(inv.getStatus())
                .createdAt(inv.getCreatedAt())
                .respondedAt(inv.getRespondedAt())
                .build();
    }
}
