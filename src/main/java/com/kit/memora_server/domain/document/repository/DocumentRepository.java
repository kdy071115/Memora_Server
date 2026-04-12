package com.kit.memora_server.domain.document.repository;

import com.kit.memora_server.domain.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByLectureId(Long lectureId);

    long countByLectureId(Long lectureId);

    boolean existsByLectureIdAndProcessingStatus(Long lectureId, String status);
}
