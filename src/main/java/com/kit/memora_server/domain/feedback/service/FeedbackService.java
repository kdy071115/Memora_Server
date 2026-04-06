package com.kit.memora_server.domain.feedback.service;

import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.feedback.dto.FeedbackRequest;
import com.kit.memora_server.domain.feedback.dto.FeedbackResponse;
import com.kit.memora_server.domain.feedback.entity.InstructorFeedback;
import com.kit.memora_server.domain.feedback.repository.InstructorFeedbackRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final InstructorFeedbackRepository feedbackRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public FeedbackResponse create(Long courseId, Long studentId, Long instructorId, FeedbackRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (!enrollmentRepository.existsByUserIdAndCourseId(studentId, courseId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        InstructorFeedback feedback = InstructorFeedback.builder()
                .course(course)
                .instructor(instructor)
                .student(student)
                .content(request.getContent())
                .build();

        return FeedbackResponse.from(feedbackRepository.save(feedback));
    }

    public List<FeedbackResponse> getByCourseAndStudent(Long courseId, Long studentId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return feedbackRepository.findByCourseIdAndStudentIdOrderByCreatedAtDesc(courseId, studentId).stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    public List<FeedbackResponse> getMyFeedback(Long studentId) {
        return feedbackRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    @Transactional
    public FeedbackResponse markAsRead(Long feedbackId, Long studentId) {
        InstructorFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getStudent().getId().equals(studentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        feedback.markAsRead();
        return FeedbackResponse.from(feedback);
    }
}
