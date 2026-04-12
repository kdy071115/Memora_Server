package com.kit.memora_server.domain.notification.service;

import com.kit.memora_server.domain.assignment.repository.SubmissionCommentRepository;
import com.kit.memora_server.domain.notification.dto.NotificationCountResponse;
import com.kit.memora_server.domain.team.entity.InvitationStatus;
import com.kit.memora_server.domain.team.repository.TeamInvitationRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final UserRepository userRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final SubmissionCommentRepository submissionCommentRepository;

    public NotificationCountResponse getCounts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        long invitations = teamInvitationRepository
                .findByInviteeIdAndStatusOrderByCreatedAtDesc(userId, InvitationStatus.PENDING)
                .size();

        LocalDateTime since = user.getFeedbackSeenAt();
        long unseenFeedback = (since == null)
                ? submissionCommentRepository.countAllUnseenForUser(userId)
                : submissionCommentRepository.countUnseenForUserSince(userId, since);

        return NotificationCountResponse.builder()
                .teamInvitations(invitations)
                .unseenFeedback(unseenFeedback)
                .total(invitations + unseenFeedback)
                .build();
    }

    /** 학생이 알림을 확인했음을 표시 — 피드백 카운트만 0 으로 만든다. */
    @Transactional
    public void markFeedbackSeen(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.markFeedbackSeen();
    }
}
