package com.kit.memora_server.domain.course.service;

import com.kit.memora_server.domain.assignment.entity.Assignment;
import com.kit.memora_server.domain.assignment.entity.Submission;
import com.kit.memora_server.domain.assignment.repository.AssignmentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionCommentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionRepository;
import com.kit.memora_server.domain.course.dto.CourseMemberDto;
import com.kit.memora_server.domain.course.dto.CourseRequest;
import com.kit.memora_server.domain.course.dto.CourseResponse;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.entity.Enrollment;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.team.entity.Team;
import com.kit.memora_server.domain.team.repository.TeamInvitationRepository;
import com.kit.memora_server.domain.team.repository.TeamMemberRepository;
import com.kit.memora_server.domain.team.repository.TeamRepository;
import com.kit.memora_server.domain.document.entity.Document;
import com.kit.memora_server.domain.document.repository.DocumentChunkRepository;
import com.kit.memora_server.domain.document.repository.DocumentRepository;
import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.lecture.repository.LectureRepository;
import com.kit.memora_server.domain.notice.repository.NoticeRepository;
import com.kit.memora_server.domain.qa.entity.QaSession;
import com.kit.memora_server.domain.qa.repository.QaMessageRepository;
import com.kit.memora_server.domain.qa.repository.QaSessionRepository;
import com.kit.memora_server.domain.quiz.entity.Quiz;
import com.kit.memora_server.domain.quiz.repository.QuizAttemptRepository;
import com.kit.memora_server.domain.quiz.repository.QuizRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.enums.UserRole;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    /** 초대 코드에 사용할 문자 (헷갈리는 I, O, 0, 1 제외). */
    private static final char[] INVITE_CODE_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int INVITE_CODE_LENGTH = 8;
    private static final int INVITE_CODE_MAX_RETRY = 10;

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionCommentRepository submissionCommentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final NoticeRepository noticeRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QaSessionRepository qaSessionRepository;
    private final QaMessageRepository qaMessageRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public CourseResponse create(Long instructorId, CourseRequest request) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .instructor(instructor)
                .inviteCode(generateUniqueInviteCode())
                .build();

        Course saved = courseRepository.save(course);
        return CourseResponse.from(saved, 0, 0, false, true);
    }

    public Page<CourseResponse> getAll(Long userId, Pageable pageable) {
        // 비로그인 사용자에게는 빈 목록을 반환합니다.
        if (userId == null) {
            return Page.empty(pageable);
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("[Course.getAll] userId={} role={}", userId, currentUser.getRole());

        if (currentUser.getRole() == UserRole.INSTRUCTOR) {
            // 교강사: 본인이 개설한 ACTIVE 강의만
            Page<Course> coursePage = courseRepository.findByInstructorIdAndStatus(userId, "ACTIVE", pageable);
            log.info("[Course.getAll] INSTRUCTOR result count={}", coursePage.getTotalElements());
            return coursePage.map(course -> toResponse(course, userId));
        }

        // 학생: 본인이 수강 등록한 ACTIVE 강의만
        // 이미 검증된 enrollmentRepository.findByUserId 를 사용해 조회한 뒤 메모리에서 페이징.
        // (학생당 수강 강의 수가 수십 개를 넘지 않으므로 충분히 효율적)
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        log.info("[Course.getAll] STUDENT enrollment rows for user {} = {}", userId, enrollments.size());

        List<Course> enrolledCourses = enrollments.stream()
                .map(e -> {
                    Course c = e.getCourse();
                    log.info("  enrollment id={} course={} status={}",
                            e.getId(),
                            c != null ? c.getId() : null,
                            c != null ? c.getStatus() : "null");
                    return c;
                })
                .filter(c -> c != null && "ACTIVE".equals(c.getStatus()))
                .toList();
        log.info("[Course.getAll] STUDENT filtered ACTIVE courses = {}", enrolledCourses.size());

        int total = enrolledCourses.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<CourseResponse> pageContent = (start >= total)
                ? List.of()
                : enrolledCourses.subList(start, end).stream()
                        .map(course -> toResponse(course, userId))
                        .toList();

        return new PageImpl<>(pageContent, pageable, total);
    }

    private CourseResponse toResponse(Course course, Long userId) {
        long students = enrollmentRepository.countByCourseId(course.getId());
        long lectures = lectureRepository.countByCourseId(course.getId());
        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId());
        boolean isOwner = course.getInstructor().getId().equals(userId);
        return CourseResponse.from(course, students, lectures, enrolled, isOwner);
    }

    public CourseResponse getById(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        long students = enrollmentRepository.countByCourseId(courseId);
        long lectures = lectureRepository.countByCourseId(courseId);
        boolean enrolled = userId != null && enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
        boolean isOwner = userId != null && course.getInstructor().getId().equals(userId);
        return CourseResponse.from(course, students, lectures, enrolled, isOwner);
    }

    @Transactional
    public CourseResponse update(Long courseId, Long instructorId, CourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        course.update(request.getTitle(), request.getDescription());

        long students = enrollmentRepository.countByCourseId(courseId);
        long lectures = lectureRepository.countByCourseId(courseId);
        return CourseResponse.from(course, students, lectures, false, true);
    }

    @Transactional
    public void delete(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 자식 → 부모 순서로 안전하게 정리합니다.
        // 1. 강의에 속한 모든 차시
        List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        for (Lecture lecture : lectures) {
            Long lectureId = lecture.getId();

            // 1-1. Quiz attempts → Quizzes
            List<Quiz> quizzes = quizRepository.findByLectureId(lectureId);
            for (Quiz quiz : quizzes) {
                quizAttemptRepository.deleteByQuizId(quiz.getId());
            }
            quizRepository.deleteAll(quizzes);

            // 1-2. QaMessages → QaSessions
            List<QaSession> sessions = qaSessionRepository.findByLectureId(lectureId);
            for (QaSession session : sessions) {
                qaMessageRepository.deleteBySessionId(session.getId());
            }
            qaSessionRepository.deleteAll(sessions);

            // 1-3. DocumentChunks → Documents
            List<Document> documents = documentRepository.findByLectureId(lectureId);
            for (Document document : documents) {
                documentChunkRepository.deleteByDocumentId(document.getId());
            }
            documentRepository.deleteAll(documents);
        }

        // 2. 차시 자체
        lectureRepository.deleteAll(lectures);

        // 3. 과제 + 제출물 + 댓글 cascade
        List<Assignment> assignments = assignmentRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        for (Assignment assignment : assignments) {
            List<Submission> submissions = submissionRepository.findByAssignmentIdOrderByCreatedAtDesc(assignment.getId());
            for (Submission submission : submissions) {
                submissionCommentRepository.deleteBySubmissionId(submission.getId());
            }
            submissionRepository.deleteByAssignmentId(assignment.getId());
        }
        assignmentRepository.deleteAll(assignments);

        // 4. 팀 + 멤버 + 초대장 cascade
        List<Team> teams = teamRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        for (Team team : teams) {
            teamInvitationRepository.deleteByTeamId(team.getId());
            teamMemberRepository.deleteByTeamId(team.getId());
        }
        teamRepository.deleteAll(teams);

        // 5. 강의 직속 자료
        noticeRepository.deleteAll(noticeRepository.findByCourseIdOrderByPinnedDescCreatedAtDesc(courseId));
        enrollmentRepository.deleteAll(enrollmentRepository.findByCourseId(courseId));

        // 6. 강의
        courseRepository.delete(course);
    }

    /**
     * 강의의 수강생 목록 (간단 정보) — 팀 초대 등에 사용.
     * 강사는 항상 볼 수 있고, 학생은 본인이 수강 중인 강의여야 한다.
     */
    public List<CourseMemberDto> getMembers(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        boolean isInstructor = course.getInstructor().getId().equals(userId);
        if (!isInstructor && !enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }
        return enrollmentRepository.findWithUserByCourseId(courseId).stream()
                .map(e -> CourseMemberDto.builder()
                        .userId(e.getUser().getId())
                        .name(e.getUser().getName())
                        .email(e.getUser().getEmail())
                        .build())
                .toList();
    }

    @Transactional
    public void enroll(Long courseId, Long userId) {
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new BusinessException(ErrorCode.ALREADY_ENROLLED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Course course = courseRepository.findById(courseId).get();

        enrollmentRepository.save(Enrollment.builder()
                .user(user)
                .course(course)
                .build());
    }

    @Transactional
    public CourseResponse enrollByCode(String inviteCode, Long userId) {
        Course course = courseRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_ENROLLED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        enrollmentRepository.save(Enrollment.builder()
                .user(user)
                .course(course)
                .build());

        long students = enrollmentRepository.countByCourseId(course.getId());
        long lectures = lectureRepository.countByCourseId(course.getId());
        return CourseResponse.from(course, students, lectures, true, false);
    }

    @Transactional
    public CourseResponse regenerateInviteCode(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        course.regenerateInviteCode(generateUniqueInviteCode());

        long students = enrollmentRepository.countByCourseId(courseId);
        long lectures = lectureRepository.countByCourseId(courseId);
        return CourseResponse.from(course, students, lectures, false, true);
    }

    private String generateUniqueInviteCode() {
        for (int i = 0; i < INVITE_CODE_MAX_RETRY; i++) {
            String code = generateRandomInviteCode();
            if (!courseRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }

    private String generateRandomInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_ALPHABET[secureRandom.nextInt(INVITE_CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }
}
