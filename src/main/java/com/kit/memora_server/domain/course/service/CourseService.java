package com.kit.memora_server.domain.course.service;

import com.kit.memora_server.domain.course.dto.CourseRequest;
import com.kit.memora_server.domain.course.dto.CourseResponse;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.entity.Enrollment;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

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

        Page<Course> coursePage;
        if (currentUser.getRole() == UserRole.INSTRUCTOR) {
            // 교강사: 본인이 개설한 ACTIVE 강의만
            coursePage = courseRepository.findByInstructorIdAndStatus(userId, "ACTIVE", pageable);
        } else {
            // 학생: 본인이 수강 등록한 ACTIVE 강의만
            coursePage = courseRepository.findEnrolledCoursesByUserId(userId, "ACTIVE", pageable);
        }

        return coursePage.map(course -> {
            long students = enrollmentRepository.countByCourseId(course.getId());
            long lectures = lectureRepository.countByCourseId(course.getId());
            boolean enrolled = enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId());
            boolean isOwner = course.getInstructor().getId().equals(userId);
            return CourseResponse.from(course, students, lectures, enrolled, isOwner);
        });
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

        // 3. 강의 직속 자료
        noticeRepository.deleteAll(noticeRepository.findByCourseIdOrderByPinnedDescCreatedAtDesc(courseId));
        enrollmentRepository.deleteAll(enrollmentRepository.findByCourseId(courseId));

        // 4. 강의
        courseRepository.delete(course);
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
