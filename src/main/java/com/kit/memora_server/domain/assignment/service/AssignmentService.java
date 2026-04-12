package com.kit.memora_server.domain.assignment.service;

import com.kit.memora_server.domain.assignment.dto.AssignmentRequest;
import com.kit.memora_server.domain.assignment.dto.AssignmentResponse;
import com.kit.memora_server.domain.assignment.dto.AssignmentStatsResponse;
import com.kit.memora_server.domain.assignment.dto.UpcomingAssignmentItem;
import com.kit.memora_server.domain.assignment.entity.Assignment;
import com.kit.memora_server.domain.assignment.entity.Submission;
import com.kit.memora_server.domain.assignment.repository.AiSubmissionFeedbackRepository;
import com.kit.memora_server.domain.assignment.repository.AssignmentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionCommentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionRepository;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.entity.Enrollment;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.team.entity.TeamMember;
import com.kit.memora_server.domain.team.repository.TeamMemberRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.enums.UserRole;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final TeamMemberRepository teamMemberRepository;

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
    public AssignmentResponse closeEarly(Long userId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        if (!assignment.getInstructor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        assignment.closeEarly();
        long count = submissionRepository.countByAssignmentId(assignmentId);
        return AssignmentResponse.from(assignment, count, false);
    }

    @Transactional
    public AssignmentResponse reopen(Long userId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        if (!assignment.getInstructor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        assignment.reopen();
        long count = submissionRepository.countByAssignmentId(assignmentId);
        return AssignmentResponse.from(assignment, count, false);
    }

    /**
     * 강사용 — 과제 제출률 + 미제출자 명단.
     * "제출했다" 정의:
     *   - 본인이 제출자(submitter) 인 Submission 이 있거나
     *   - 본인이 멤버인 팀(team) 으로 제출된 Submission 이 있거나
     */
    public AssignmentStatsResponse getStats(Long userId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        Course course = assignment.getCourse();
        if (!course.getInstructor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 1. 강의 수강생 (eager-load 로 한 번에)
        List<Enrollment> enrollments = enrollmentRepository.findWithUserByCourseId(course.getId());
        long totalStudents = enrollments.size();

        // 2. 제출자 ID 집합 — 개인 제출 + 팀 제출 모두 풀어 멤버 ID 까지 포함
        List<Submission> submissions = submissionRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId);
        Set<Long> submittedUserIds = new HashSet<>();
        for (Submission s : submissions) {
            submittedUserIds.add(s.getSubmitter().getId());
            if (s.getTeam() != null) {
                List<TeamMember> members = teamMemberRepository.findByTeamId(s.getTeam().getId());
                for (TeamMember m : members) {
                    submittedUserIds.add(m.getUser().getId());
                }
            }
        }

        // 수강생 중에서만 카운트 (강사가 직접 제출했거나 탈퇴한 학생 등 제외)
        long submittedStudents = enrollments.stream()
                .filter(e -> submittedUserIds.contains(e.getUser().getId()))
                .count();

        int rate = totalStudents == 0 ? 0 : (int) Math.round(((double) submittedStudents / (double) totalStudents) * 100.0);

        List<AssignmentStatsResponse.MissingStudent> missing = enrollments.stream()
                .filter(e -> !submittedUserIds.contains(e.getUser().getId()))
                .map(e -> AssignmentStatsResponse.MissingStudent.builder()
                        .userId(e.getUser().getId())
                        .name(e.getUser().getName())
                        .email(e.getUser().getEmail())
                        .build())
                .toList();

        return AssignmentStatsResponse.builder()
                .totalStudents(totalStudents)
                .submittedStudents(submittedStudents)
                .submissionRate(rate)
                .missing(missing)
                .build();
    }

    /**
     * 학생 대시보드용 — 내가 수강 중인 강의의 마감 임박 과제 (가까운 순, 최대 limit 개).
     * dueDate 가 없는 과제는 제외. 마감 지난 과제도 제외 (이미 끝났으므로).
     */
    public List<UpcomingAssignmentItem> getUpcomingForStudent(Long userId, int limit) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        List<Course> activeCourses = enrollments.stream()
                .map(Enrollment::getCourse)
                .filter(c -> c != null && "ACTIVE".equals(c.getStatus()))
                .toList();
        if (activeCourses.isEmpty()) return List.of();

        List<Assignment> upcoming = assignmentRepository.findUpcomingByCourses(activeCourses, LocalDateTime.now());

        // 학생이 이미 제출한 과제 ID 집합 (과제 단위 — 팀 제출도 본인이 제출자였으면 카운트)
        Set<Long> submittedIds = new HashSet<>();
        for (Assignment a : upcoming) {
            boolean mine = submissionRepository.findByAssignmentIdOrderByCreatedAtDesc(a.getId())
                    .stream().anyMatch(s -> s.getSubmitter().getId().equals(userId));
            if (mine) submittedIds.add(a.getId());
        }

        return upcoming.stream()
                .limit(limit)
                .map(a -> UpcomingAssignmentItem.from(a, submittedIds.contains(a.getId())))
                .toList();
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
