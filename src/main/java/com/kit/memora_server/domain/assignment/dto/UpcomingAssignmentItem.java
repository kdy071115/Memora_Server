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
@Schema(description = "학생 대시보드용 마감 임박 과제 한 건")
public class UpcomingAssignmentItem {

    private Long id;
    private String title;

    private Long courseId;
    private String courseTitle;

    private LocalDateTime dueDate;
    private boolean mySubmissionExists;

    public static UpcomingAssignmentItem from(Assignment a, boolean submitted) {
        return UpcomingAssignmentItem.builder()
                .id(a.getId())
                .title(a.getTitle())
                .courseId(a.getCourse().getId())
                .courseTitle(a.getCourse().getTitle())
                .dueDate(a.getDueDate())
                .mySubmissionExists(submitted)
                .build();
    }
}
