package com.kit.memora_server.domain.assignment.repository;

import com.kit.memora_server.domain.assignment.entity.SubmissionComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SubmissionCommentRepository extends JpaRepository<SubmissionComment, Long> {
    List<SubmissionComment> findBySubmissionIdOrderByCreatedAtAsc(Long submissionId);
    void deleteBySubmissionId(Long submissionId);

    /**
     * 학생이 받은 미확인 피드백 댓글 수를 센다.
     *  - 학생이 본인 제출물(submitter = userId)에 달린 댓글 중
     *  - 작성자가 본인이 아닌 것 (강사 또는 다른 사람)
     *  - 마지막 확인 시각(since) 이후에 작성된 것
     * since 가 null 이면 모든 시간 범위를 포함.
     */
    @Query("SELECT COUNT(c) FROM SubmissionComment c " +
           "WHERE c.submission.submitter.id = :userId " +
           "AND c.author.id <> :userId " +
           "AND (:since IS NULL OR c.createdAt > :since)")
    long countUnseenForUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
