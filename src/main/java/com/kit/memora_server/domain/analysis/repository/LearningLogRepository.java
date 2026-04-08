package com.kit.memora_server.domain.analysis.repository;

import com.kit.memora_server.domain.analysis.entity.LearningLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LearningLogRepository extends JpaRepository<LearningLog, Long> {

    List<LearningLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<LearningLog> findByUserIdAndLectureId(Long userId, Long lectureId);

    List<LearningLog> findByUserIdAndActivityTypeOrderByCreatedAtDesc(Long userId, String activityType);

    List<LearningLog> findByUserIdAndLectureIdAndActivityTypeOrderByCreatedAtDesc(Long userId, Long lectureId, String activityType);

    @Query("SELECT COALESCE(SUM(l.duration), 0) FROM LearningLog l WHERE l.user.id = :userId")
    long sumDurationByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(l.duration), 0) FROM LearningLog l WHERE l.user.id = :userId AND l.lecture.course.id = :courseId")
    long sumDurationByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("SELECT l FROM LearningLog l WHERE l.user.id = :userId AND l.lecture.course.id = :courseId ORDER BY l.createdAt DESC")
    List<LearningLog> findByUserIdAndCourseIdOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("SELECT l FROM LearningLog l WHERE l.lecture.course.id = :courseId ORDER BY l.createdAt DESC")
    List<LearningLog> findByCourseIdOrderByCreatedAtDesc(@Param("courseId") Long courseId);

    @Query("SELECT COALESCE(MAX(l.createdAt), NULL) FROM LearningLog l WHERE l.user.id = :userId AND l.lecture.course.id = :courseId")
    LocalDateTime findMaxCreatedAtByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    /** 학생이 한 번이라도 학습 활동을 기록한 차시(lecture)의 개수 — 강의 진도율 계산용. */
    @Query("SELECT COUNT(DISTINCT l.lecture.id) FROM LearningLog l WHERE l.user.id = :userId AND l.lecture.course.id = :courseId")
    long countDistinctVisitedLecturesByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
