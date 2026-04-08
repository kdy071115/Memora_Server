package com.kit.memora_server.domain.quiz.dto;

import com.kit.memora_server.domain.quiz.entity.QuizAttempt;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 학생 본인의 퀴즈 풀이 기록(점수/일시) 만 노출하는 안전한 DTO.
 * 정답(correctAnswer), 학생 답안(userAnswer), 해설(explanation)은 포함하지 않는다 —
 * 기록을 보고 답을 외우지 않도록 의도적으로 제외.
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "퀴즈 풀이 기록 한 건 (점수/일시 요약)")
public class QuizAttemptHistoryItem {

    @Schema(description = "시도 ID", example = "1")
    private Long attemptId;

    @Schema(description = "퀴즈 ID", example = "12")
    private Long quizId;

    @Schema(description = "퀴즈 질문 (요약 표시용)", example = "광합성의 명반응이 일어나는 장소는?")
    private String question;

    @Schema(description = "개념 태그", example = "광합성")
    private String conceptTag;

    @Schema(description = "난이도", example = "MEDIUM")
    private String difficulty;

    @Schema(description = "점수 (0~100)", example = "80")
    private Integer score;

    @Schema(description = "정답 여부", example = "true")
    private Boolean isCorrect;

    @Schema(description = "시도 일시", example = "2026-04-08T13:42:01")
    private LocalDateTime attemptedAt;

    public static QuizAttemptHistoryItem from(QuizAttempt attempt) {
        return QuizAttemptHistoryItem.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .question(attempt.getQuiz().getQuestion())
                .conceptTag(attempt.getQuiz().getConceptTag())
                .difficulty(attempt.getQuiz().getDifficulty())
                .score(attempt.getScore())
                .isCorrect(attempt.getIsCorrect())
                .attemptedAt(attempt.getAttemptedAt())
                .build();
    }
}
