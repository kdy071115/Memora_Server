package com.kit.memora_server.domain.document.service;

import com.kit.memora_server.domain.document.dto.DocumentResponse;
import com.kit.memora_server.domain.document.dto.DocumentSummaryResponse;
import com.kit.memora_server.domain.document.entity.Document;
import com.kit.memora_server.domain.document.entity.DocumentChunk;
import com.kit.memora_server.domain.document.repository.DocumentChunkRepository;
import com.kit.memora_server.domain.document.repository.DocumentRepository;
import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.lecture.repository.LectureRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.infra.ai.AiServerClient;
import com.kit.memora_server.infra.ai.dto.AiDocumentCallbackRequest;
import com.kit.memora_server.infra.ai.dto.AiDocumentProcessRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final LectureRepository lectureRepository;
    private final S3Service s3Service;
    private final AiServerClient aiServerClient;

    @Transactional
    public DocumentResponse upload(Long lectureId, MultipartFile file) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        String originalName = file.getOriginalFilename();
        String fileType = getFileType(originalName);
        String storedPath = s3Service.upload(file, "documents/" + lectureId);

        Document document = Document.builder()
                .lecture(lecture)
                .originalName(originalName)
                .storedPath(storedPath)
                .fileType(fileType)
                .fileSize(file.getSize())
                .build();

        Document saved = documentRepository.save(document);

        // 비동기로 AI 서버에 처리 요청
        aiServerClient.requestDocumentProcess(AiDocumentProcessRequest.builder()
                .documentId(saved.getId())
                .lectureId(lectureId)
                .storedPath(storedPath)
                .callbackUrl("/api/internal/documents/" + saved.getId() + "/callback")
                .build());

        saved.updateProcessingStatus("PROCESSING");

        return DocumentResponse.from(saved);
    }

    public List<DocumentResponse> getByLecture(Long lectureId) {
        return documentRepository.findByLectureId(lectureId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public DocumentSummaryResponse getSummary(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!"COMPLETED".equals(document.getProcessingStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_PROCESSING);
        }

        return DocumentSummaryResponse.from(document);
    }

    @Transactional
    public void handleCallback(Long documentId, AiDocumentCallbackRequest request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        if ("COMPLETED".equals(request.getStatus())) {
            document.completeProcessing(request.getSummary(), request.getChunks().size());

            List<DocumentChunk> chunks = request.getChunks().stream()
                    .map(c -> DocumentChunk.builder()
                            .document(document)
                            .chunkIndex(c.getChunkIndex())
                            .content(c.getContent())
                            .pageNumber(c.getPageNumber())
                            .tokenCount(c.getTokenCount())
                            .embeddingId(c.getEmbeddingId())
                            .build())
                    .toList();

            documentChunkRepository.saveAll(chunks);
            log.info("문서 처리 완료: documentId={}, chunks={}", documentId, chunks.size());
        } else {
            document.failProcessing();
            log.error("문서 처리 실패: documentId={}", documentId);
        }
    }

    private String getFileType(String filename) {
        if (filename == null) return "UNKNOWN";
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase();
        return switch (ext) {
            case "PDF" -> "PDF";
            case "TXT" -> "TXT";
            case "DOCX", "DOC" -> "DOCX";
            default -> "UNKNOWN";
        };
    }
}
