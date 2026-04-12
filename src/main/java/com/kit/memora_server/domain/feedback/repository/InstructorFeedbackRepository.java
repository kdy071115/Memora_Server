package com.kit.memora_server.domain.feedback.repository;

import com.kit.memora_server.domain.feedback.entity.InstructorFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstructorFeedbackRepository extends JpaRepository<InstructorFeedback, Long> {

    List<InstructorFeedback> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<InstructorFeedback> findByCourseIdAndStudentIdOrderByCreatedAtDesc(Long courseId, Long studentId);
}
