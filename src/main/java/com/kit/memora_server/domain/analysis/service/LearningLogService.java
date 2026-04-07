package com.kit.memora_server.domain.analysis.service;

import com.kit.memora_server.domain.analysis.dto.LearningLogRequest;
import com.kit.memora_server.domain.analysis.entity.LearningLog;
import com.kit.memora_server.domain.analysis.repository.LearningLogRepository;
import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.lecture.repository.LectureRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningLogService {

    private final LearningLogRepository learningLogRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;

    /**
     * 학습 페이지/QA/퀴즈 화면에서 일정 주기마다 호출되는 heartbeat.
     * 한 호출이 가져온 duration(초)을 그대로 LearningLog 한 행으로 저장합니다.
     * 누적은 sumDurationByUserId 같은 합산 쿼리가 처리합니다.
     */
    @Transactional
    public void recordHeartbeat(Long userId, Long lectureId, LearningLogRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        String activityType = request.getActivityType();
        if (activityType == null || activityType.isBlank()) {
            activityType = "LEARN";
        }

        LearningLog log = LearningLog.builder()
                .user(user)
                .lecture(lecture)
                .activityType(activityType)
                .duration(request.getDuration())
                .build();

        learningLogRepository.save(log);
    }
}
