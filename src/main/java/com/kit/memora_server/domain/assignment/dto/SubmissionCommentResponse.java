package com.kit.memora_server.domain.assignment.dto;

import com.kit.memora_server.domain.assignment.entity.SubmissionComment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "제출물 댓글")
public class SubmissionCommentResponse {

    private Long id;
    private Long authorId;
    private String authorName;
    private String authorRole;
    private String content;
    private LocalDateTime createdAt;

    public static SubmissionCommentResponse from(SubmissionComment c) {
        return SubmissionCommentResponse.builder()
                .id(c.getId())
                .authorId(c.getAuthor().getId())
                .authorName(c.getAuthor().getName())
                .authorRole(c.getAuthor().getRole() != null ? c.getAuthor().getRole().name() : null)
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
