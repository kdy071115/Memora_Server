package com.kit.memora_server.domain.course.controller;

import com.kit.memora_server.domain.course.dto.CourseRequest;
import com.kit.memora_server.domain.course.dto.CourseResponse;
import com.kit.memora_server.domain.course.service.CourseService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.common.PageResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Course", description = "강의 API")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "강의 생성")
    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CourseRequest request) {
        CourseResponse response = courseService.create(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("강의가 생성되었습니다.", response));
    }

    @Operation(summary = "강의 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getAll(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = user != null ? user.getId() : null;
        Page<CourseResponse> page = courseService.getAll(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @Operation(summary = "강의 상세")
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> getById(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(courseService.getById(courseId, userId)));
    }

    @Operation(summary = "강의 수정")
    @PutMapping("/{courseId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<CourseResponse>> update(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.update(courseId, user.getId(), request)));
    }

    @Operation(summary = "강의 삭제")
    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user) {
        courseService.delete(courseId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("강의가 삭제되었습니다."));
    }

    @Operation(summary = "수강 등록")
    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<ApiResponse<Void>> enroll(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user) {
        courseService.enroll(courseId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("수강 등록되었습니다."));
    }
}
