package com.kit.memora_server.domain.audionote.controller;

import com.kit.memora_server.domain.audionote.dto.AudioNoteResponse;
import com.kit.memora_server.domain.audionote.service.AudioNoteService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AudioNote", description = "강의 음성 → 자동 노트화")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AudioNoteController {

    private final AudioNoteService audioNoteService;

    @Operation(summary = "강사용 — 강의 음성 업로드 + 자동 트랜스크립션",
            description = "차시당 1개. 같은 차시에 다시 업로드하면 기존 노트를 덮어씀. " +
                    "AI 서버에서 faster-whisper 로 트랜스크립트를 만들고 Claude 로 요약·챕터 분리.")
    @PostMapping(value = "/api/lectures/{lectureId}/audio-note", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AudioNoteResponse>> upload(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(
                "음성 노트가 생성되었습니다.",
                audioNoteService.uploadAndTranscribe(user.getId(), lectureId, file)
        ));
    }

    @Operation(summary = "차시의 음성 노트 조회 (강사·수강생)")
    @GetMapping("/api/lectures/{lectureId}/audio-note")
    public ResponseEntity<ApiResponse<AudioNoteResponse>> get(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(audioNoteService.getByLecture(user.getId(), lectureId)));
    }

    @Operation(summary = "음성 노트 삭제 (강사)")
    @DeleteMapping("/api/lectures/{lectureId}/audio-note")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user) {
        audioNoteService.delete(user.getId(), lectureId);
        return ResponseEntity.ok(ApiResponse.ok("음성 노트가 삭제되었습니다."));
    }
}
