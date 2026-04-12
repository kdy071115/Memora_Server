package com.kit.memora_server.domain.team.dto;

import com.kit.memora_server.domain.team.entity.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "팀 멤버")
public class TeamMemberDto {

    private Long userId;
    private String name;
    private String email;
    private LocalDateTime joinedAt;

    public static TeamMemberDto from(TeamMember m) {
        return TeamMemberDto.builder()
                .userId(m.getUser().getId())
                .name(m.getUser().getName())
                .email(m.getUser().getEmail())
                .joinedAt(m.getJoinedAt())
                .build();
    }
}
