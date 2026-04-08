package com.kit.memora_server.domain.assignment.repository;

import com.kit.memora_server.domain.assignment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);
    List<Submission> findBySubmitterIdOrderByCreatedAtDesc(Long submitterId);
    long countByAssignmentId(Long assignmentId);
    void deleteByAssignmentId(Long assignmentId);

    /** 학생이 해당 강의의 과제에 대해 제출한 distinct 과제 수 — 과제 제출률 계산용. */
    @Query("SELECT COUNT(DISTINCT s.assignment.id) FROM Submission s WHERE s.submitter.id = :userId AND s.assignment.course.id = :courseId")
    long countDistinctSubmittedAssignmentsByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
