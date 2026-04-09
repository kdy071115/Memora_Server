package com.kit.memora_server.domain.audionote.repository;

import com.kit.memora_server.domain.audionote.entity.AudioNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AudioNoteRepository extends JpaRepository<AudioNote, Long> {
    Optional<AudioNote> findByLectureId(Long lectureId);
    void deleteByLectureId(Long lectureId);
}
