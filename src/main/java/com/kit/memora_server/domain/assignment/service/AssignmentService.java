package com.kit.memora_server.domain.assignment.service;

import com.kit.memora_server.domain.assignment.dto.AssignmentRequest;
import com.kit.memora_server.domain.assignment.dto.AssignmentResponse;
import com.kit.memora_server.domain.assignment.entity.Assignment;
import com.kit.memora_server.domain.assignment.repository.AiSubmissionFeedbackRepository;
import com.kit.memora_server.domain.assignment.repository.AssignmentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionCommentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionRepository;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.enums.UserRole;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionCommentRepository submissionCommentRepository;
    private final AiSubmissionFeedbackRepository aiSubmissionFeedbackRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public AssignmentResponse create(Long userId, Long courseId, AssignmentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.getInstructor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Assignment assignment = Assignment.builder()
                .course(course)
                .instructor(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .allowTeamSubmission(Boolean.TRUE.equals(request.getAllowTeamSubmission()))
                .build();
        assignmentRepository.save(assignment);
        return AssignmentResponse.from(assignment, 0L, false);
    }

    public List<AssignmentResponse> listByCourse(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        boolean isInstructor = course.getInstructor().getId().equals(userId);
        if (!isInstructor && user.getRole() == UserRole.STUDENT) {
            boolean enrolled = enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
            if (!enrolled) throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        return assignmentRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(a -> {
                    long count = submissionRepository.countByAssignmentId(a.getId());
                    boolean mine = submissionRepository.findByAssignmentIdOrderByCreatedAtDesc(a.getId())
                            .stream().anyMatch(s -> s.getSubmitter().getId().equals(userId));
                    return AssignmentResponse.from(a, count, mine);
                })
                .toList();
    }

    public AssignmentResponse get(Long userId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        Long courseId = assignment.getCourse().getId();
        boolean isInstructor = assignment.getCourse().getInstructor().getId().equals(userId);
        if (!isInstructor) {
            boolean enrolled = enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
            if (!enrolled) throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        long count = submissionRepository.countByAssignmentId(assignmentId);
        boolean mine = submissionRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId)
                .stream().anyMatch(s -> s.getSubmitter().getId().equals(userId));
        return AssignmentResponse.from(assignment, count, mine);
    }

    @Transactional
    public AssignmentResponse update(Long userId, Long assignmentId, AssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        if (!assignment.getInstructor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        assignment.update(request.getTitle(), request.getDescription(), request.getDueDate(), request.getAllowTeamSubmission());
        long count = submissionRepository.countByAssignmentId(assignmentId);
        return AssignmentResponse.from(assignment, count, false);
    }

    @Transactional
    public void delete(Long userId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        if (!assignment.getInstructor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // AI 캐시 → 댓글 → 제출 → 과제 순으로 삭제
        submissionRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId)
                .forEach(s -> {
                    aiSubmissionFeedbackRepository.deleteBySubmissionId(s.getId());
                    submissionCommentRepository.deleteBySubmissionId(s.getId());
                });
        submissionRepository.deleteByAssignmentId(assignmentId);
        assignmentRepository.delete(assignment);
    }
}
