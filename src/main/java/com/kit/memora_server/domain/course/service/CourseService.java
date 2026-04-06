package com.kit.memora_server.domain.course.service;

import com.kit.memora_server.domain.course.dto.CourseRequest;
import com.kit.memora_server.domain.course.dto.CourseResponse;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.entity.Enrollment;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.lecture.repository.LectureRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

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
        return courseRepository.findByStatus("ACTIVE", pageable)
                .map(course -> {
                    long students = enrollmentRepository.countByCourseId(course.getId());
                    long lectures = lectureRepository.countByCourseId(course.getId());
                    boolean enrolled = userId != null && enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId());
                    boolean isOwner = userId != null && course.getInstructor().getId().equals(userId);
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
