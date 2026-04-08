package com.kit.memora_server.domain.quiz.repository;

import com.kit.memora_server.domain.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByLectureId(Long lectureId);

    List<Quiz> findByLectureIdAndDifficulty(Long lectureId, String difficulty);

    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.lecture.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);
}
