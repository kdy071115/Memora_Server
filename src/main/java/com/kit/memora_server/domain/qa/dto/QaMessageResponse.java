package com.kit.memora_server.domain.qa.dto;

import com.kit.memora_server.domain.qa.entity.QaMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QaMessageResponse {

    private Long id;
    private String role;
    private String content;
    private List<QaSourceDto> sources;
    private LocalDateTime createdAt;

    public static QaMessageResponse from(QaMessage message, List<QaSourceDto> sources) {
        return QaMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .sources(sources)
                .createdAt(message.getCreatedAt())
                .build();
    }
}
