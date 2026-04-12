package com.kit.memora_server.domain.lecture.service;

import com.kit.memora_server.domain.audionote.repository.AudioNoteRepository;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.document.entity.Document;
import com.kit.memora_server.domain.document.repository.DocumentChunkRepository;
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
    private final DocumentChunkRepository documentChunkRepository;
    private final AudioNoteRepository audioNoteRepository;

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

    @Transactional
    public void delete(Long lectureId, Long instructorId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        if (!lecture.getCourse().getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 차시에 속한 자료(문서)와 청크 + 음성 노트를 모두 정리한 뒤 차시를 삭제합니다.
        audioNoteRepository.deleteByLectureId(lectureId);

        List<Document> documents = documentRepository.findByLectureId(lectureId);
        for (Document doc : documents) {
            documentChunkRepository.deleteByDocumentId(doc.getId());
        }
        documentRepository.deleteAll(documents);

        lectureRepository.delete(lecture);
    }

    @Transactional
    public LectureResponse update(Long lectureId, Long instructorId, LectureRequest request) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        if (!lecture.getCourse().getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        lecture.update(request.getTitle(), request.getDescription());

        long docCount = documentRepository.countByLectureId(lecture.getId());
        boolean hasCompleted = documentRepository.existsByLectureIdAndProcessingStatus(lecture.getId(), "COMPLETED");
        return LectureResponse.from(lecture, docCount, hasCompleted);
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
