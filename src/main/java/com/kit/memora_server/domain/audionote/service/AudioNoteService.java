package com.kit.memora_server.domain.audionote.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kit.memora_server.domain.audionote.dto.AudioNoteResponse;
import com.kit.memora_server.domain.audionote.entity.AudioNote;
import com.kit.memora_server.domain.audionote.repository.AudioNoteRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.lecture.repository.LectureRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.infra.ai.AiServerClient;
import com.kit.memora_server.infra.ai.dto.AiAudioTranscribeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 강사가 강의 음성을 업로드하면 AI 서버로 보내 트랜스크립트 + 요약 + 챕터를 만들고 차시에 저장. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AudioNoteService {

    private static final long MAX_AUDIO_BYTES = 100L * 1024 * 1024; // 100MB

    private final AudioNoteRepository audioNoteRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AiServerClient aiServerClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public AudioNoteResponse uploadAndTranscribe(Long userId, Long lectureId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        // 강사 본인 강의만
        if (!lecture.getCourse().getInstructor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (file.getSize() > MAX_AUDIO_BYTES) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        AiAudioTranscribeResponse aiRes = aiServerClient.transcribeAudio(bytes, file.getOriginalFilename(), "ko");

        final String chaptersJson = serializeChapters(aiRes.getChapters());

        AudioNote saved = audioNoteRepository.findByLectureId(lectureId)
                .map(existing -> {
                    existing.update(
                            aiRes.getFullTranscript() != null ? aiRes.getFullTranscript() : "",
                            aiRes.getSummary() != null ? aiRes.getSummary() : "",
                            chaptersJson,
                            aiRes.getDurationSec() != null ? aiRes.getDurationSec() : 0.0
                    );
                    return existing;
                })
                .orElseGet(() -> audioNoteRepository.save(
                        AudioNote.builder()
                                .lecture(lecture)
                                .uploader(user)
                                .originalFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio")
                                .durationSec(aiRes.getDurationSec() != null ? aiRes.getDurationSec() : 0.0)
                                .transcript(aiRes.getFullTranscript() != null ? aiRes.getFullTranscript() : "")
                                .summary(aiRes.getSummary() != null ? aiRes.getSummary() : "")
                                .chaptersJson(chaptersJson)
                                .build()
                ));

        return toResponse(saved);
    }

    /** 학생도 본인이 수강 중인 강의의 차시 노트를 볼 수 있다. */
    public AudioNoteResponse getByLecture(Long userId, Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        Long courseId = lecture.getCourse().getId();
        boolean isInstructor = lecture.getCourse().getInstructor().getId().equals(userId);
        if (!isInstructor && !enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        return audioNoteRepository.findByLectureId(lectureId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public void delete(Long userId, Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));
        if (!lecture.getCourse().getInstructor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        audioNoteRepository.deleteByLectureId(lectureId);
    }

    private String serializeChapters(List<?> chapters) {
        try {
            return objectMapper.writeValueAsString(chapters != null ? chapters : Collections.emptyList());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private AudioNoteResponse toResponse(AudioNote note) {
        List<AudioNoteResponse.Chapter> chapters = new ArrayList<>();
        if (note.getChaptersJson() != null && !note.getChaptersJson().isBlank()) {
            try {
                List<java.util.Map<String, Object>> raw = objectMapper.readValue(
                        note.getChaptersJson(), new TypeReference<>() {}
                );
                for (java.util.Map<String, Object> c : raw) {
                    chapters.add(AudioNoteResponse.Chapter.builder()
                            .title(c.get("title") != null ? c.get("title").toString() : "")
                            .startSec(c.get("startSec") instanceof Number n ? n.doubleValue() : 0.0)
                            .summary(c.get("summary") != null ? c.get("summary").toString() : "")
                            .build());
                }
            } catch (JsonProcessingException e) {
                log.warn("AudioNote 챕터 파싱 실패: {}", e.getMessage());
            }
        }
        return AudioNoteResponse.builder()
                .id(note.getId())
                .lectureId(note.getLecture().getId())
                .originalFileName(note.getOriginalFileName())
                .durationSec(note.getDurationSec())
                .transcript(note.getTranscript())
                .summary(note.getSummary())
                .chapters(chapters)
                .createdAt(note.getCreatedAt())
                .build();
    }
}
