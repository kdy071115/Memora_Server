package com.kit.memora_server.domain.lecture.dto;

import com.kit.memora_server.domain.lecture.entity.Lecture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LectureResponse {

    private Long id;
    private String title;
    private String description;
    private Integer orderIndex;
    private long documentCount;
    private boolean hasCompletedDocuments;

    public static LectureResponse from(Lecture lecture, long documentCount, boolean hasCompleted) {
        return LectureResponse.builder()
                .id(lecture.getId())
                .title(lecture.getTitle())
                .description(lecture.getDescription())
                .orderIndex(lecture.getOrderIndex())
                .documentCount(documentCount)
                .hasCompletedDocuments(hasCompleted)
                .build();
    }
}
