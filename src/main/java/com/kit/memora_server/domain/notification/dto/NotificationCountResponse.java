package com.kit.memora_server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "헤더 종 아이콘에서 사용하는 알림 카운트 묶음")
public class NotificationCountResponse {

    @Schema(description = "받은 미확인 팀 초대장 개수", example = "2")
    private long teamInvitations;

    @Schema(description = "내 제출물에 달린 미확인 피드백 댓글 개수", example = "3")
    private long unseenFeedback;

    @Schema(description = "두 카운트 합계", example = "5")
    private long total;
}
