package com.kit.memora_server.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kit.memora_server.domain.course.entity.Course;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "강의 응답")
public class CourseResponse {

    @Schema(description = "강의 ID", example = "1")
    private Long id;

    @Schema(description = "강의 제목", example = "인공지능 개론")
    private String title;

    @Schema(description = "강의 설명", example = "AI 기초를 다룹니다.")
    private String description;

    @Schema(description = "담당 교직자")
    private InstructorInfo instructor;

    @Schema(description = "수강생 수", example = "35")
    private long studentCount;

    @Schema(description = "차시 수", example = "12")
    private long lectureCount;

    @Schema(description = "강의 상태", example = "OPEN")
    private String status;

    @Schema(description = "현재 사용자의 수강 여부", example = "true")
    private boolean isEnrolled;

    @Schema(description = "생성 시각", example = "2026-03-01T09:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "초대 코드 (소유 강사에게만 노출, 수강생 응답에서는 필드 자체가 제외됨)",
            example = "A7KQ3M2P", nullable = true)
    private String inviteCode;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "강의 담당 교직자 정보")
    public static class InstructorInfo {

        @Schema(description = "교직자 사용자 ID", example = "2")
        private Long id;

        @Schema(description = "교직자 이름", example = "김교수")
        private String name;
    }

    public static CourseResponse from(Course course, long studentCount, long lectureCount, boolean isEnrolled) {
        return from(course, studentCount, lectureCount, isEnrolled, false);
    }

    public static CourseResponse from(Course course, long studentCount, long lectureCount,
                                      boolean isEnrolled, boolean includeInviteCode) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .instructor(InstructorInfo.builder()
                        .id(course.getInstructor().getId())
                        .name(course.getInstructor().getName())
                        .build())
                .studentCount(studentCount)
                .lectureCount(lectureCount)
                .status(course.getStatus())
                .isEnrolled(isEnrolled)
                .createdAt(course.getCreatedAt())
                .inviteCode(includeInviteCode ? course.getInviteCode() : null)
                .build();
    }
}
