package com.kit.memora_server.domain.atrisk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "강사가 위험 학생에게 보낼 케어 메시지 초안")
public class CareMessageResponse {
    private String message;
    private List<String> suggestedActions;
}
