package com.kit.memora_server.domain.quiz.repository;

import com.kit.memora_server.domain.quiz.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUserIdAndQuiz_LectureIdOrderByAttemptedAtDesc(Long userId, Long lectureId);

    List<QuizAttempt> findByUserIdOrderByAttemptedAtDesc(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndIsCorrectTrue(Long userId);

    boolean existsByQuizId(Long quizId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.lecture.course.id = :courseId")
    List<QuizAttempt> findByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
