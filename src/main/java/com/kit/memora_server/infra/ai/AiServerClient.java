package com.kit.memora_server.infra.ai;

import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.infra.ai.dto.AiDocumentProcessRequest;
import com.kit.memora_server.infra.ai.dto.AiQaRequest;
import com.kit.memora_server.infra.ai.dto.AiQaResponse;
import com.kit.memora_server.infra.ai.dto.AiQuizGenerateRequest;
import com.kit.memora_server.infra.ai.dto.AiQuizGenerateResponse;
import com.kit.memora_server.infra.ai.dto.AiQuizGradeRequest;
import com.kit.memora_server.infra.ai.dto.AiQuizGradeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServerClient {

    private final WebClient aiWebClient;

    public void requestDocumentProcess(AiDocumentProcessRequest request) {
        aiWebClient.post()
                .uri("/ai/documents/process")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("AI 서버 문서 처리 요청 실패: {}", e.getMessage()))
                .subscribe();
    }

    public AiQaResponse askQuestion(AiQaRequest request) {
        try {
            return aiWebClient.post()
                    .uri("/ai/qa/ask")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiQaResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("AI 서버 QA 요청 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        } catch (Exception e) {
            log.error("AI 서버 QA 요청 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    public AiQuizGenerateResponse generateQuiz(AiQuizGenerateRequest request) {
        try {
            return aiWebClient.post()
                    .uri("/ai/quiz/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiQuizGenerateResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("AI 서버 퀴즈 생성 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        } catch (Exception e) {
            log.error("AI 서버 퀴즈 생성 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    public AiQuizGradeResponse gradeQuiz(AiQuizGradeRequest request) {
        try {
            return aiWebClient.post()
                    .uri("/ai/quiz/grade")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiQuizGradeResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("AI 서버 채점 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        } catch (Exception e) {
            log.error("AI 서버 채점 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }
}
