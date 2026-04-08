package com.kit.memora_server.domain.team.dto;

import com.kit.memora_server.domain.team.entity.Team;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "팀 정보")
public class TeamResponse {

    private Long id;
    private Long courseId;
    private String courseTitle;
    private Long leaderId;
    private String leaderName;
    private String name;
    private String description;
    private List<TeamMemberDto> members;
    private LocalDateTime createdAt;

    public static TeamResponse from(Team team, List<TeamMemberDto> members) {
        return TeamResponse.builder()
                .id(team.getId())
                .courseId(team.getCourse().getId())
                .courseTitle(team.getCourse().getTitle())
                .leaderId(team.getLeader().getId())
                .leaderName(team.getLeader().getName())
                .name(team.getName())
                .description(team.getDescription())
                .members(members)
                .createdAt(team.getCreatedAt())
                .build();
    }
}
