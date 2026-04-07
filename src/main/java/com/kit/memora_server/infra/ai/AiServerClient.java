package com.kit.memora_server.infra.ai;

import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.infra.ai.dto.AiDocumentProcessRequest;
import com.kit.memora_server.infra.ai.dto.AiQaRequest;
import com.kit.memora_server.infra.ai.dto.AiQaResponse;
import com.kit.memora_server.infra.ai.dto.AiAnalysisRequest;
import com.kit.memora_server.infra.ai.dto.AiAnalysisResponse;
import com.kit.memora_server.infra.ai.dto.AiQuizGenerateRequest;
import com.kit.memora_server.infra.ai.dto.AiQuizGenerateResponse;
import com.kit.memora_server.infra.ai.dto.AiQuizGradeRequest;
import com.kit.memora_server.infra.ai.dto.AiQuizGradeResponse;
import com.kit.memora_server.infra.ai.dto.AiSelfExplainRequest;
import com.kit.memora_server.infra.ai.dto.AiSelfExplainResponse;
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

    public AiSelfExplainResponse evaluateSelfExplanation(AiSelfExplainRequest request) {
        try {
            return aiWebClient.post()
                    .uri("/ai/self-explain")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiSelfExplainResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("AI 서버 자기 설명 평가 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        } catch (Exception e) {
            log.error("AI 서버 자기 설명 평가 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    public AiAnalysisResponse analyzeLearning(AiAnalysisRequest request) {
        try {
            return aiWebClient.post()
                    .uri("/ai/analysis/learning")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiAnalysisResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("AI 서버 학습 분석 실패: {}", e.getMessage());
            // 학습 분석은 실패 시에도 통계 데이터는 반환할 수 있도록 fallback 응답 사용
            return AiAnalysisResponse.builder()
                    .diagnosis("현재 학습 데이터 기반 진단을 일시적으로 제공할 수 없습니다.")
                    .recommendations(java.util.Collections.emptyList())
                    .motivation("꾸준한 학습이 가장 큰 무기입니다.")
                    .build();
        }
    }
}
