package com.kit.memora_server.domain.team.repository;

import com.kit.memora_server.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByCourseIdOrderByCreatedAtDesc(Long courseId);
}
