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
     * 학생이 받은 미확인 피드백 댓글 수를 센다 — since 시점 이후에 작성된 것만.
     * (PostgreSQL 이 `:since IS NULL` 표현의 파라미터 타입을 추론하지 못해
     *  since=null 케이스는 별도 메서드로 분리.)
     */
    @Query("SELECT COUNT(c) FROM SubmissionComment c " +
           "WHERE c.submission.submitter.id = :userId " +
           "AND c.author.id <> :userId " +
           "AND c.createdAt > :since")
    long countUnseenForUserSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /** 마지막 확인 시각이 없는 경우 — 본인 제출물에 달린 모든 타인 댓글 카운트. */
    @Query("SELECT COUNT(c) FROM SubmissionComment c " +
           "WHERE c.submission.submitter.id = :userId " +
           "AND c.author.id <> :userId")
    long countAllUnseenForUser(@Param("userId") Long userId);
}
