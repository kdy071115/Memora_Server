package com.kit.memora_server.domain.assignment.repository;

import com.kit.memora_server.domain.assignment.entity.SubmissionComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionCommentRepository extends JpaRepository<SubmissionComment, Long> {
    List<SubmissionComment> findBySubmissionIdOrderByCreatedAtAsc(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
