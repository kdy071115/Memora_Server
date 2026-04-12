package com.kit.memora_server.domain.assignment.repository;

import com.kit.memora_server.domain.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.kit.memora_server.domain.course.entity.Course;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourseIdOrderByCreatedAtDesc(Long courseId);
    long countByCourseId(Long courseId);

    /**
     * 학생 대시보드용 — 주어진 강의들 중 마감일이 미래(또는 지정 시점 이후)인 과제만 가까운 순으로.
     * dueDate 가 null 인 과제(마감 없음)는 제외.
     */
    @Query("SELECT a FROM Assignment a WHERE a.course IN :courses AND a.dueDate IS NOT NULL AND a.dueDate >= :now ORDER BY a.dueDate ASC")
    List<Assignment> findUpcomingByCourses(
            @Param("courses") Collection<Course> courses,
            @Param("now") LocalDateTime now
    );
}
