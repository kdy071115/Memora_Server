package com.kit.memora_server.domain.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "교직자 문제 수정 요청 — null 필드는 일부 유지되고 options/explanation/conceptTag는 null 전달 시 제거됨")
public class QuizUpdateRequest {

    @Schema(description = "문제 본문 (null 이면 기존 값 유지)")
    private String question;

    @Schema(description = "문제 유형 (null 이면 기존 값 유지)", allowableValues = {"MULTIPLE_CHOICE", "SHORT_ANSWER", "ESSAY"})
    private String quizType;

    @Schema(description = "객관식 보기 (null 이면 제거)")
    private List<String> options;

    @Schema(description = "정답 (null 이면 기존 값 유지)")
    private String correctAnswer;

    @Schema(description = "해설 (null 이면 제거)")
    private String explanation;

    @Schema(description = "난이도 (null 이면 기존 값 유지)", allowableValues = {"EASY", "MEDIUM", "HARD"})
    private String difficulty;

    @Schema(description = "개념 태그 (null 이면 제거)")
    private String conceptTag;
}
