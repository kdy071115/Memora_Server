package com.kit.memora_server.domain.qa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaSourceDto {
    private Long documentId;
    private String documentName;
    private Integer pageNumber;
    private String preview;
}
