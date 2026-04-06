package com.kit.memora_server.domain.course.repository;

import com.kit.memora_server.domain.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findByStatus(String status, Pageable pageable);

    Page<Course> findByInstructorId(Long instructorId, Pageable pageable);

    Optional<Course> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
