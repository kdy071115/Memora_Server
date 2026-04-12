package com.kit.memora_server.domain.qa.dto;

import com.kit.memora_server.domain.qa.entity.QaSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QaSessionResponse {

    private Long id;
    private Long lectureId;
    private String title;
    private List<QaMessageResponse> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static QaSessionResponse of(QaSession session, List<QaMessageResponse> messages) {
        return QaSessionResponse.builder()
                .id(session.getId())
                .lectureId(session.getLecture().getId())
                .title(session.getTitle())
                .messages(messages)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
