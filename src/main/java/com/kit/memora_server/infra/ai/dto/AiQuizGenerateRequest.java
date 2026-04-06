package com.kit.memora_server.infra.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiQuizGenerateRequest {
    private Long lectureId;
    private Integer count;
    private List<String> types;
    private String difficulty;
    private List<String> conceptTags;
}
