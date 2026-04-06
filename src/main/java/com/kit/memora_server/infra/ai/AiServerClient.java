package com.kit.memora_server.infra.ai;

import com.kit.memora_server.infra.ai.dto.AiDocumentProcessRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

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

    public Map<String, Object> askQuestion(Map<String, Object> request) {
        return aiWebClient.post()
                .uri("/ai/qa/ask")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> (Map<String, Object>) m)
                .block();
    }

    public Object generateQuiz(Map<String, Object> request) {
        return aiWebClient.post()
                .uri("/ai/quiz/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Map<String, Object> gradeQuiz(Map<String, Object> request) {
        return aiWebClient.post()
                .uri("/ai/quiz/grade")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> (Map<String, Object>) m)
                .block();
    }

    public Map<String, Object> analyzeLearning(Map<String, Object> request) {
        return aiWebClient.post()
                .uri("/ai/analysis/learning")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> (Map<String, Object>) m)
                .block();
    }
}
