package com.kit.memora_server.domain.notice.dto;

import com.kit.memora_server.domain.notice.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "공지사항 응답")
public class NoticeResponse {

    @Schema(description = "공지 ID", example = "10")
    private Long id;

    @Schema(description = "강의 ID", example = "1")
    private Long courseId;

    @Schema(description = "공지 제목", example = "중간고사 일정 안내")
    private String title;

    @Schema(description = "공지 본문", example = "중간고사는 4월 20일 오후 2시에 진행됩니다.")
    private String content;

    @Schema(description = "상단 고정 여부", example = "true")
    private boolean pinned;

    @Schema(description = "작성자(교직자) ID", example = "2")
    private Long authorId;

    @Schema(description = "작성자(교직자) 이름", example = "김교수")
    private String authorName;

    @Schema(description = "작성 시각", example = "2026-04-06T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각", example = "2026-04-06T14:30:00")
    private LocalDateTime updatedAt;

    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .courseId(notice.getCourse().getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .pinned(notice.isPinned())
                .authorId(notice.getAuthor().getId())
                .authorName(notice.getAuthor().getName())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}
