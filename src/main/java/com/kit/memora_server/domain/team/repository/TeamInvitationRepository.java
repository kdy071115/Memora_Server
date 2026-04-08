package com.kit.memora_server.domain.team.repository;

import com.kit.memora_server.domain.team.entity.InvitationStatus;
import com.kit.memora_server.domain.team.entity.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    List<TeamInvitation> findByInviteeIdAndStatusOrderByCreatedAtDesc(Long inviteeId, InvitationStatus status);
    List<TeamInvitation> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    boolean existsByTeamIdAndInviteeIdAndStatus(Long teamId, Long inviteeId, InvitationStatus status);
    void deleteByTeamId(Long teamId);
}
