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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;

    /** AiFeedbackResponse 직렬화 결과 — 그대로 클라이언트에 돌려준다 */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    public void updatePayload(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
