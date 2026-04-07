package com.kit.memora_server.domain.course.repository;

import com.kit.memora_server.domain.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findByStatus(String status, Pageable pageable);

    Page<Course> findByInstructorId(Long instructorId, Pageable pageable);

    Page<Course> findByInstructorIdAndStatus(Long instructorId, String status, Pageable pageable);

    @Query("SELECT c FROM Course c JOIN Enrollment e ON e.course = c " +
            "WHERE e.user.id = :userId AND c.status = :status")
    Page<Course> findEnrolledCoursesByUserId(@Param("userId") Long userId,
                                             @Param("status") String status,
                                             Pageable pageable);

    Optional<Course> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
