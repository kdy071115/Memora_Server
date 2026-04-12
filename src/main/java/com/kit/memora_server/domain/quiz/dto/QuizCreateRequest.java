package com.kit.memora_server.domain.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "교직자 수동 문제 생성 요청")
public class QuizCreateRequest {

    @NotBlank(message = "문제 본문은 필수입니다.")
    @Schema(description = "문제 본문", example = "RAG에서 Retrieval 단계의 주요 목적은?", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    @NotBlank(message = "문제 유형은 필수입니다.")
    @Schema(description = "문제 유형", example = "MULTIPLE_CHOICE",
            allowableValues = {"MULTIPLE_CHOICE", "SHORT_ANSWER", "ESSAY"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String quizType;

    @Schema(description = "객관식 보기 (주관식은 null)",
            example = "[\"A. 모델 학습\", \"B. 문서 검색\", \"C. 응답 생성\", \"D. 전처리\"]")
    private List<String> options;

    @NotBlank(message = "정답은 필수입니다.")
    @Schema(description = "정답", example = "B", requiredMode = Schema.RequiredMode.REQUIRED)
    private String correctAnswer;

    @Schema(description = "해설", example = "Retrieval 단계는 관련 문서를 벡터 DB에서 검색합니다.")
    private String explanation;

    @Schema(description = "난이도", example = "MEDIUM", allowableValues = {"EASY", "MEDIUM", "HARD"}, defaultValue = "MEDIUM")
    private String difficulty = "MEDIUM";

    @Schema(description = "개념 태그", example = "RAG")
    private String conceptTag;
}
