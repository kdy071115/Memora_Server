package com.kit.memora_server.domain.lecture.service;

import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.document.repository.DocumentRepository;
import com.kit.memora_server.domain.lecture.dto.LectureRequest;
import com.kit.memora_server.domain.lecture.dto.LectureResponse;
import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.lecture.repository.LectureRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureService {

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public LectureResponse create(Long courseId, Long instructorId, LectureRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        int nextOrder = (int) lectureRepository.countByCourseId(courseId) + 1;

        Lecture lecture = Lecture.builder()
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .orderIndex(nextOrder)
                .build();

        Lecture saved = lectureRepository.save(lecture);
        return LectureResponse.from(saved, 0, false);
    }

    public List<LectureResponse> getByCourse(Long courseId) {
        return lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
                .map(lecture -> {
                    long docCount = documentRepository.countByLectureId(lecture.getId());
                    boolean hasCompleted = documentRepository.existsByLectureIdAndProcessingStatus(lecture.getId(), "COMPLETED");
                    return LectureResponse.from(lecture, docCount, hasCompleted);
                })
                .toList();
    }
}
