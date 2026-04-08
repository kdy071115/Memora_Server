package com.kit.memora_server.domain.assignment.entity;

import com.kit.memora_server.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 강사가 생성한 AI 피드백 초안의 캐시.
 * 학생에게는 절대 노출되지 않으며, 강사 본인이 같은 제출물을 다시 열었을 때 재생성 비용을
 * 아끼고 결과를 그대로 다시 볼 수 있도록 하기 위해 저장한다.
 *
 * 제출물 본문이나 첨부가 수정되면 캐시는 무효화(삭제) 한다.
 */
@Entity
@Table(name = "ai_submission_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiSubmissionFeedback extends BaseEntity {

    public enum Status {
        PENDING,    // 백그라운드 생성 중
        READY,      // 완료, payloadJson 사용 가능
        FAILED      // 실패, payloadJson 에 에러 메시지
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    /** AiFeedbackResponse 직렬화 결과 — 완료된 경우. PENDING 상태에서는 null */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    public void markReady(String payloadJson) {
        this.payloadJson = payloadJson;
        this.status = Status.READY;
    }

    public void markFailed(String errorMessage) {
        this.payloadJson = errorMessage;
        this.status = Status.FAILED;
    }

    public void resetToPending() {
        this.status = Status.PENDING;
        this.payloadJson = null;
    }
}
