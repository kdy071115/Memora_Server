package com.kit.memora_server.domain.quiz.controller;

import com.kit.memora_server.domain.quiz.dto.QuizAttemptResponse;
import com.kit.memora_server.domain.quiz.dto.QuizCreateRequest;
import com.kit.memora_server.domain.quiz.dto.QuizGenerateRequest;
import com.kit.memora_server.domain.quiz.dto.QuizResponse;
import com.kit.memora_server.domain.quiz.dto.QuizSubmitRequest;
import com.kit.memora_server.domain.quiz.dto.QuizSubmitResponse;
import com.kit.memora_server.domain.quiz.dto.QuizUpdateRequest;
import com.kit.memora_server.domain.quiz.service.QuizService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Quiz", description = "퀴즈 생성/풀이/채점 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "문제 자동 생성")
    @PostMapping("/api/lectures/{lectureId}/quizzes/generate")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> generate(
            @PathVariable Long lectureId,
            @Valid @RequestBody QuizGenerateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(quizService.generate(lectureId, request)));
    }

    @Operation(summary = "문제 수동 생성 (교직자)")
    @PostMapping("/api/lectures/{lectureId}/quizzes")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<QuizResponse>> create(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody QuizCreateRequest request) {
        QuizResponse response = quizService.create(lectureId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("문제가 생성되었습니다.", response));
    }

    @Operation(summary = "문제 수정 (교직자)")
    @PutMapping("/api/quizzes/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<QuizResponse>> update(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody QuizUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "문제가 수정되었습니다.",
                quizService.update(quizId, user.getId(), request)
        ));
    }

    @Operation(summary = "문제 삭제 (교직자)")
    @DeleteMapping("/api/quizzes/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails user) {
        quizService.delete(quizId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("문제가 삭제되었습니다."));
    }

    @Operation(summary = "문제 목록 조회")
    @GetMapping("/api/lectures/{lectureId}/quizzes")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getByLecture(
            @PathVariable Long lectureId,
            @RequestParam(required = false) String difficulty) {
        return ResponseEntity.ok(ApiResponse.ok(quizService.getQuizzesByLecture(lectureId, difficulty)));
    }

    @Operation(summary = "답안 제출 + 채점")
    @PostMapping("/api/quizzes/{quizId}/submit")
    public ResponseEntity<ApiResponse<QuizSubmitResponse>> submit(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody QuizSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(quizService.submit(quizId, user.getId(), request)));
    }

    @Operation(summary = "내 풀이 기록")
    @GetMapping("/api/lectures/{lectureId}/quizzes/attempts")
    public ResponseEntity<ApiResponse<List<QuizAttemptResponse>>> getMyAttempts(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(quizService.getMyAttempts(user.getId(), lectureId)));
    }
}
