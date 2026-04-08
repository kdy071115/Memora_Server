package com.kit.memora_server.domain.assignment.repository;

import com.kit.memora_server.domain.assignment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);
    List<Submission> findBySubmitterIdOrderByCreatedAtDesc(Long submitterId);
    long countByAssignmentId(Long assignmentId);
    void deleteByAssignmentId(Long assignmentId);
}
