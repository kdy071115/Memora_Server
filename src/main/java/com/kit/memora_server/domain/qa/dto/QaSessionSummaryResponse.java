package com.kit.memora_server.domain.qa.dto;

import com.kit.memora_server.domain.qa.entity.QaSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class QaSessionSummaryResponse {

    private Long id;
    private Long lectureId;
    private String lectureTitle;
    private String title;
    private String lastMessage;
    private long messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static QaSessionSummaryResponse of(QaSession session, String lastMessage, long messageCount) {
        return QaSessionSummaryResponse.builder()
                .id(session.getId())
                .lectureId(session.getLecture().getId())
                .lectureTitle(session.getLecture().getTitle())
                .title(session.getTitle())
                .lastMessage(lastMessage)
                .messageCount(messageCount)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
