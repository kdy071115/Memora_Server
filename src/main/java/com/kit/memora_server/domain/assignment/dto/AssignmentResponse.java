package com.kit.memora_server.domain.assignment.dto;

import com.kit.memora_server.domain.assignment.entity.Assignment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "과제 정보")
public class AssignmentResponse {

    private Long id;
    private Long courseId;
    private String courseTitle;
    private Long instructorId;
    private String instructorName;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private boolean allowTeamSubmission;
    private long submissionCount;
    private boolean mySubmissionExists;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AssignmentResponse from(Assignment a, long submissionCount, boolean mySubmissionExists) {
        return AssignmentResponse.builder()
                .id(a.getId())
                .courseId(a.getCourse() != null ? a.getCourse().getId() : null)
                .courseTitle(a.getCourse() != null ? a.getCourse().getTitle() : null)
                .instructorId(a.getInstructor() != null ? a.getInstructor().getId() : null)
                .instructorName(a.getInstructor() != null ? a.getInstructor().getName() : null)
                .title(a.getTitle())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .allowTeamSubmission(a.isAllowTeamSubmission())
                .submissionCount(submissionCount)
                .mySubmissionExists(mySubmissionExists)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
