package com.kit.memora_server.domain.notice.service;

import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.notice.dto.NoticeRequest;
import com.kit.memora_server.domain.notice.dto.NoticeResponse;
import com.kit.memora_server.domain.notice.entity.Notice;
import com.kit.memora_server.domain.notice.repository.NoticeRepository;
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
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public NoticeResponse create(Long courseId, Long instructorId, NoticeRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        User author = userRepository.findById(instructorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Notice notice = Notice.builder()
                .course(course)
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .pinned(request.isPinned())
                .build();

        return NoticeResponse.from(noticeRepository.save(notice));
    }

    public List<NoticeResponse> getByCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        boolean isOwner = course.getInstructor().getId().equals(userId);
        boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
        if (!isOwner && !isEnrolled) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        return noticeRepository.findByCourseIdOrderByPinnedDescCreatedAtDesc(courseId).stream()
                .map(NoticeResponse::from)
                .toList();
    }

    @Transactional
    public NoticeResponse update(Long noticeId, Long instructorId, NoticeRequest request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        if (!notice.getAuthor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        notice.update(request.getTitle(), request.getContent(), request.isPinned());
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void delete(Long noticeId, Long instructorId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        if (!notice.getAuthor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        noticeRepository.delete(notice);
    }
}
