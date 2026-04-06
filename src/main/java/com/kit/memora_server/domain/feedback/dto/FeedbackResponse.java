package com.kit.memora_server.domain.feedback.dto;

import com.kit.memora_server.domain.feedback.entity.InstructorFeedback;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "교직자 피드백 응답")
public class FeedbackResponse {

    @Schema(description = "피드백 ID", example = "5")
    private Long id;

    @Schema(description = "강의 ID", example = "1")
    private Long courseId;

    @Schema(description = "강의명", example = "인공지능 개론")
    private String courseTitle;

    @Schema(description = "작성 교직자 ID", example = "2")
    private Long instructorId;

    @Schema(description = "작성 교직자 이름", example = "김교수")
    private String instructorName;

    @Schema(description = "수신 학생 ID", example = "7")
    private Long studentId;

    @Schema(description = "수신 학생 이름", example = "김학생")
    private String studentName;

    @Schema(description = "피드백 본문", example = "최근 퀴즈 성적이 많이 올랐어요. 계속 이 페이스로 가세요!")
    private String content;

    @Schema(description = "학생 읽음 여부", example = "false")
    private boolean readByStudent;

    @Schema(description = "작성 시각", example = "2026-04-06T14:30:00")
    private LocalDateTime createdAt;

    public static FeedbackResponse from(InstructorFeedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .courseId(feedback.getCourse().getId())
                .courseTitle(feedback.getCourse().getTitle())
                .instructorId(feedback.getInstructor().getId())
                .instructorName(feedback.getInstructor().getName())
                .studentId(feedback.getStudent().getId())
                .studentName(feedback.getStudent().getName())
                .content(feedback.getContent())
                .readByStudent(feedback.isReadByStudent())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
