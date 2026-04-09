package com.kit.memora_server.domain.audionote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "차시의 강의 음성 노트")
public class AudioNoteResponse {

    private Long id;
    private Long lectureId;
    private String originalFileName;
    private Double durationSec;
    private String transcript;
    private String summary;
    private List<Chapter> chapters;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Chapter {
        private String title;
        private Double startSec;
        private String summary;
    }
}
