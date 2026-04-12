package com.kit.memora_server.infra.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAssignmentFeedbackRequest {
    private String assignmentTitle;
    private String assignmentDescription;
    private String studentName;
    private String submissionContent;
    private String attachmentName;
    /** 첨부 파일 본문 base64 — null 이면 첨부 없음 */
    private String attachmentBase64;
}
