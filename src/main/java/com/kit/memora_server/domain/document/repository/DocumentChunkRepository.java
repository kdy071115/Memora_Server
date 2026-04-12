package com.kit.memora_server.domain.document.repository;

import com.kit.memora_server.domain.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    void deleteByDocumentId(Long documentId);
}
