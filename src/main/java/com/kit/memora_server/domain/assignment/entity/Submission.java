package com.kit.memora_server.domain.assignment.entity;

import com.kit.memora_server.domain.team.entity.Team;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "submissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Submission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    /** 실제로 제출 버튼을 누른 학생 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id", nullable = false)
    private User submitter;

    /** 팀 제출인 경우의 팀 — null 이면 개인 제출 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 첨부 파일 (옵션) — S3 key 또는 로컬 절대경로 */
    private String attachmentPath;
    private String attachmentName;
    private Long attachmentSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubmissionVisibility visibility = SubmissionVisibility.PRIVATE;

    public void update(String content, SubmissionVisibility visibility, Team team) {
        if (content != null && !content.isBlank()) this.content = content;
        if (visibility != null) this.visibility = visibility;
        // team 은 null 도 허용 (팀 제출 → 개인 제출 전환)
        this.team = team;
    }

    public void replaceAttachment(String path, String name, Long size) {
        this.attachmentPath = path;
        this.attachmentName = name;
        this.attachmentSize = size;
    }

    public void clearAttachment() {
        this.attachmentPath = null;
        this.attachmentName = null;
        this.attachmentSize = null;
    }
}
