package com.kit.memora_server.domain.lecture.controller;

import com.kit.memora_server.domain.lecture.dto.LectureRequest;
import com.kit.memora_server.domain.lecture.dto.LectureResponse;
import com.kit.memora_server.domain.lecture.service.LectureService;
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

@Tag(name = "Lecture", description = "차시 API")
@RestController
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;

    @Operation(summary = "차시 생성")
    @PostMapping("/api/courses/{courseId}/lectures")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LectureResponse>> create(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody LectureRequest request) {
        LectureResponse response = lectureService.create(courseId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("차시가 생성되었습니다.", response));
    }

    @Operation(summary = "차시 목록")
    @GetMapping("/api/courses/{courseId}/lectures")
    public ResponseEntity<ApiResponse<List<LectureResponse>>> getByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.ok(lectureService.getByCourse(courseId)));
    }

    @Operation(summary = "차시 수정")
    @PutMapping("/api/lectures/{lectureId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LectureResponse>> update(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody LectureRequest request) {
        LectureResponse response = lectureService.update(lectureId, user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("차시가 수정되었습니다.", response));
    }

    @Operation(summary = "차시 삭제")
    @DeleteMapping("/api/lectures/{lectureId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal CustomUserDetails user) {
        lectureService.delete(lectureId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("차시가 삭제되었습니다."));
    }
}
