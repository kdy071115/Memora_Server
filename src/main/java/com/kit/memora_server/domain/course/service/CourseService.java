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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourseResponse create(Long instructorId, CourseRequest request) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .instructor(instructor)
                .build();

        Course saved = courseRepository.save(course);
        return CourseResponse.from(saved, 0, 0, false);
    }

    public Page<CourseResponse> getAll(Long userId, Pageable pageable) {
        return courseRepository.findByStatus("ACTIVE", pageable)
                .map(course -> {
                    long students = enrollmentRepository.countByCourseId(course.getId());
                    long lectures = lectureRepository.countByCourseId(course.getId());
                    boolean enrolled = userId != null && enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId());
                    return CourseResponse.from(course, students, lectures, enrolled);
                });
    }

    public CourseResponse getById(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        long students = enrollmentRepository.countByCourseId(courseId);
        long lectures = lectureRepository.countByCourseId(courseId);
        boolean enrolled = userId != null && enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
        return CourseResponse.from(course, students, lectures, enrolled);
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
        return CourseResponse.from(course, students, lectures, false);
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
}
