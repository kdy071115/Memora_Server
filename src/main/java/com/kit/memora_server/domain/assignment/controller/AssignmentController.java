package com.kit.memora_server.domain.assignment.controller;

import com.kit.memora_server.domain.assignment.dto.AssignmentRequest;
import com.kit.memora_server.domain.assignment.dto.AssignmentResponse;
import com.kit.memora_server.domain.assignment.service.AssignmentService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Assignment", description = "과제 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @Operation(summary = "과제 생성 (강사)")
    @PostMapping("/api/courses/{courseId}/assignments")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> create(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("과제가 생성되었습니다.", assignmentService.create(user.getId(), courseId, request)));
    }

    @Operation(summary = "강의의 과제 목록")
    @GetMapping("/api/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> list(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.listByCourse(user.getId(), courseId)));
    }

    @Operation(summary = "과제 상세")
    @GetMapping("/api/assignments/{assignmentId}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> get(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.get(user.getId(), assignmentId)));
    }

    @Operation(summary = "과제 수정 (강사)")
    @PutMapping("/api/assignments/{assignmentId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> update(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("과제가 수정되었습니다.", assignmentService.update(user.getId(), assignmentId, request)));
    }

    @Operation(summary = "과제 삭제 (강사)")
    @DeleteMapping("/api/assignments/{assignmentId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserDetails user) {
        assignmentService.delete(user.getId(), assignmentId);
        return ResponseEntity.ok(ApiResponse.ok("과제가 삭제되었습니다."));
    }
}
