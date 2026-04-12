package com.kit.memora_server.domain.assignment.dto;

import com.kit.memora_server.domain.assignment.entity.Submission;
import com.kit.memora_server.domain.assignment.entity.SubmissionVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "제출물 응답")
public class SubmissionResponse {

    private Long id;
    private Long assignmentId;
    private String assignmentTitle;

    private Long submitterId;
    private String submitterName;

    private Long teamId;
    private String teamName;

    private String content;

    private String attachmentName;
    private Long attachmentSize;
    private boolean hasAttachment;

    private SubmissionVisibility visibility;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 댓글은 상세 조회 응답에서만 채워짐. 목록 응답에서는 null */
    private List<SubmissionCommentResponse> comments;

    public static SubmissionResponse from(Submission s, List<SubmissionCommentResponse> comments) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .assignmentId(s.getAssignment().getId())
                .assignmentTitle(s.getAssignment().getTitle())
                .submitterId(s.getSubmitter().getId())
                .submitterName(s.getSubmitter().getName())
                .teamId(s.getTeam() != null ? s.getTeam().getId() : null)
                .teamName(s.getTeam() != null ? s.getTeam().getName() : null)
                .content(s.getContent())
                .attachmentName(s.getAttachmentName())
                .attachmentSize(s.getAttachmentSize())
                .hasAttachment(s.getAttachmentPath() != null)
                .visibility(s.getVisibility())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .comments(comments)
                .build();
    }
}
