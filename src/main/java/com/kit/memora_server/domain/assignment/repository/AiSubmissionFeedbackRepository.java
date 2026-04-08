package com.kit.memora_server.domain.assignment.repository;

import com.kit.memora_server.domain.assignment.entity.AiSubmissionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiSubmissionFeedbackRepository extends JpaRepository<AiSubmissionFeedback, Long> {
    Optional<AiSubmissionFeedback> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
