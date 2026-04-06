package com.kit.memora_server.domain.analysis.repository;

import com.kit.memora_server.domain.analysis.entity.LearningLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LearningLogRepository extends JpaRepository<LearningLog, Long> {

    List<LearningLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<LearningLog> findByUserIdAndLectureId(Long userId, Long lectureId);

    @Query("SELECT COALESCE(SUM(l.duration), 0) FROM LearningLog l WHERE l.user.id = :userId")
    long sumDurationByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(l.duration), 0) FROM LearningLog l WHERE l.user.id = :userId AND l.lecture.course.id = :courseId")
    long sumDurationByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
