package com.kit.memora_server.domain.assignment.repository;

import com.kit.memora_server.domain.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourseIdOrderByCreatedAtDesc(Long courseId);
}
