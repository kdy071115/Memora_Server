package com.kit.memora_server.domain.qa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.lecture.repository.LectureRepository;
import com.kit.memora_server.domain.qa.dto.QaMessageRequest;
import com.kit.memora_server.domain.qa.dto.QaMessageResponse;
import com.kit.memora_server.domain.qa.dto.QaSessionResponse;
import com.kit.memora_server.domain.qa.dto.QaSessionSummaryResponse;
import com.kit.memora_server.domain.qa.dto.QaSourceDto;
import com.kit.memora_server.domain.qa.entity.QaMessage;
import com.kit.memora_server.domain.qa.entity.QaSession;
import com.kit.memora_server.domain.qa.repository.QaMessageRepository;
import com.kit.memora_server.domain.qa.repository.QaSessionRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.infra.ai.AiServerClient;
import com.kit.memora_server.infra.ai.dto.AiQaRequest;
import com.kit.memora_server.infra.ai.dto.AiQaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QaService {

    private static final int HISTORY_LIMIT = 6;

    private final QaSessionRepository qaSessionRepository;
    private final QaMessageRepository qaMessageRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final AiServerClient aiServerClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public QaSessionResponse createSession(Long userId, Long lectureId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        QaSession session = QaSession.builder()
                .user(user)
                .lecture(lecture)
                .title(lecture.getTitle() + " 질문")
                .build();
        QaSession saved = qaSessionRepository.save(session);

        return QaSessionResponse.of(saved, Collections.emptyList());
    }

    public List<QaSessionSummaryResponse> getMySessions(Long userId, Long lectureId) {
        List<QaSession> sessions = (lectureId != null)
                ? qaSessionRepository.findByUserIdAndLectureIdOrderByUpdatedAtDesc(userId, lectureId)
                : qaSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);

        return sessions.stream()
                .map(s -> {
                    List<QaMessage> messages = qaMessageRepository.findBySessionIdOrderByCreatedAtAsc(s.getId());
                    String last = messages.isEmpty() ? null : messages.get(messages.size() - 1).getContent();
                    if (last != null && last.length() > 80) {
                        last = last.substring(0, 80) + "...";
                    }
                    return QaSessionSummaryResponse.of(s, last, messages.size());
                })
                .toList();
    }

    public QaSessionResponse getSession(Long userId, Long sessionId) {
        QaSession session = qaSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        List<QaMessage> messages = qaMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<QaMessageResponse> messageResponses = messages.stream()
                .map(m -> QaMessageResponse.from(m, deserializeSources(m.getSourceChunks())))
                .toList();

        return QaSessionResponse.of(session, messageResponses);
    }

    @Transactional
    public QaMessageResponse sendMessage(Long userId, Long sessionId, QaMessageRequest request) {
        QaSession session = qaSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 1. 사용자 메시지 저장
        QaMessage userMessage = QaMessage.builder()
                .session(session)
                .role("USER")
                .content(request.getContent())
                .build();
        qaMessageRepository.save(userMessage);

        // 2. 이전 대화 히스토리 구성 (방금 저장한 사용자 메시지는 제외)
        List<QaMessage> previousMessages = qaMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<AiQaRequest.HistoryMessage> history = new ArrayList<>();
        int start = Math.max(0, previousMessages.size() - 1 - HISTORY_LIMIT);
        for (int i = start; i < previousMessages.size() - 1; i++) {
            QaMessage m = previousMessages.get(i);
            history.add(AiQaRequest.HistoryMessage.builder()
                    .role(m.getRole())
                    .content(m.getContent())
                    .build());
        }

        // 3. AI 서버 호출
        AiQaRequest aiRequest = AiQaRequest.builder()
                .lectureId(session.getLecture().getId())
                .question(request.getContent())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : "MEDIUM")
                .history(history)
                .build();
        AiQaResponse aiResponse = aiServerClient.askQuestion(aiRequest);

        // 4. 어시스턴트 메시지 저장
        List<QaSourceDto> sources = aiResponse.getSources() == null
                ? Collections.emptyList()
                : aiResponse.getSources().stream()
                    .map(s -> QaSourceDto.builder()
                            .documentId(s.getDocumentId())
                            .documentName(s.getDocumentName())
                            .pageNumber(s.getPageNumber())
                            .preview(s.getPreview())
                            .build())
                    .toList();

        QaMessage assistantMessage = QaMessage.builder()
                .session(session)
                .role("ASSISTANT")
                .content(aiResponse.getAnswer())
                .sourceChunks(serializeSources(sources))
                .build();
        QaMessage savedAssistant = qaMessageRepository.save(assistantMessage);

        return QaMessageResponse.from(savedAssistant, sources);
    }

    private String serializeSources(List<QaSourceDto> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.warn("source 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    private List<QaSourceDto> deserializeSources(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QaSourceDto.class)
            );
        } catch (JsonProcessingException e) {
            log.warn("source 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
