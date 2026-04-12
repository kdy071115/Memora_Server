package com.kit.memora_server.domain.document.entity;

import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Document extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false, length = 500)
    private String storedPath;

    @Column(nullable = false, length = 20)
    private String fileType;

    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Builder.Default
    private Integer chunkCount = 0;

    @Column(length = 20)
    @Builder.Default
    private String processingStatus = "PENDING";

    public void updateProcessingStatus(String status) {
        this.processingStatus = status;
    }

    public void completeProcessing(String summary, int chunkCount) {
        this.summary = summary;
        this.chunkCount = chunkCount;
        this.processingStatus = "COMPLETED";
    }

    public void failProcessing() {
        this.processingStatus = "FAILED";
    }
}
