package com.kit.memora_server.domain.course.dto;

import com.kit.memora_server.domain.course.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CourseResponse {

    private Long id;
    private String title;
    private String description;
    private InstructorInfo instructor;
    private long studentCount;
    private long lectureCount;
    private String status;
    private boolean isEnrolled;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class InstructorInfo {
        private Long id;
        private String name;
    }

    public static CourseResponse from(Course course, long studentCount, long lectureCount, boolean isEnrolled) {
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
                .build();
    }
}
