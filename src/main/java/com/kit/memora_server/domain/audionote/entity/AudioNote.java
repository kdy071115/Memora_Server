package com.kit.memora_server.domain.audionote.entity;

import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 강의 음성 → faster-whisper 트랜스크립트 + AI 요약 + 챕터 분리 결과.
 * 차시당 1개. 챕터는 JSON 으로 직렬화 저장.
 */
@Entity
@Table(name = "audio_notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AudioNote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false, length = 300)
    private String originalFileName;

    @Column(nullable = false)
    private Double durationSec;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String transcript;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    /** AudioChapter 리스트의 JSON 직렬화 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String chaptersJson;

    public void update(String transcript, String summary, String chaptersJson, Double durationSec) {
        this.transcript = transcript;
        this.summary = summary;
        this.chaptersJson = chaptersJson;
        this.durationSec = durationSec;
    }
}
