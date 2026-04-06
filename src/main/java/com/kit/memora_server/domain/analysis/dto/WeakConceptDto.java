package com.kit.memora_server.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WeakConceptDto {
    private String concept;
    private Double correctRate;
    private long attemptCount;
}
