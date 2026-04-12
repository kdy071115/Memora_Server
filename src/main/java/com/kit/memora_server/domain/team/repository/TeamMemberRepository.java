package com.kit.memora_server.domain.team.repository;

import com.kit.memora_server.domain.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @Query("SELECT m FROM TeamMember m JOIN FETCH m.user WHERE m.team.id = :teamId")
    List<TeamMember> findByTeamIdWithUser(@Param("teamId") Long teamId);

    List<TeamMember> findByTeamId(Long teamId);

    List<TeamMember> findByUserId(Long userId);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    void deleteByTeamId(Long teamId);

    void deleteByTeamIdAndUserId(Long teamId, Long userId);
}
