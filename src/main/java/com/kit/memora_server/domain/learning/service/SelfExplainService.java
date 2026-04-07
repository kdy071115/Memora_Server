package com.kit.memora_server.domain.learning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kit.memora_server.domain.analysis.entity.LearningLog;
import com.kit.memora_server.domain.analysis.repository.LearningLogRepository;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.learning.dto.SelfExplainHistoryItem;
import com.kit.memora_server.domain.learning.dto.SelfExplainRequest;
import com.kit.memora_server.domain.learning.dto.SelfExplainResponse;
import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.lecture.repository.LectureRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.infra.ai.AiServerClient;
import com.kit.memora_server.infra.ai.dto.AiSelfExplainRequest;
import com.kit.memora_server.infra.ai.dto.AiSelfExplainResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfExplainService {

    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final LearningLogRepository learningLogRepository;
    private final AiServerClient aiServerClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public SelfExplainResponse evaluate(Long userId, Long lectureId, SelfExplainRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        AiSelfExplainResponse aiResponse = aiServerClient.evaluateSelfExplanation(
                AiSelfExplainRequest.builder()
                        .lectureId(lectureId)
                        .explanation(request.getExplanation())
                        .focusTopic(request.getFocusTopic())
                        .build()
        );

        // 학습 로그로 저장 (회고 / 분석 기능에서 재활용)
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "SELF_EXPLAIN");
            metadata.put("focusTopic", request.getFocusTopic());
            metadata.put("explanation", request.getExplanation());
            metadata.put("overallScore", aiResponse.getOverallScore());
            metadata.put("grade", aiResponse.getGrade());
            metadata.put("strengths", aiResponse.getStrengths());
            metadata.put("missingConcepts", aiResponse.getMissingConcepts());
            metadata.put("misconceptions", aiResponse.getMisconceptions());

            String metadataJson = objectMapper.writeValueAsString(metadata);
            // 자기 설명 한 번을 60초 학습 활동으로 카운트
            LearningLog logEntry = LearningLog.builder()
                    .user(user)
                    .lecture(lecture)
                    .activityType("SELF_EXPLAIN")
                    .duration(60)
                    .metadata(metadataJson)
                    .build();
            learningLogRepository.save(logEntry);
        } catch (JsonProcessingException e) {
            log.warn("자기 설명 metadata 직렬화 실패 (저장 스킵): {}", e.getMessage());
        }

        return SelfExplainResponse.builder()
                .overallScore(aiResponse.getOverallScore())
                .grade(aiResponse.getGrade())
                .strengths(safe(aiResponse.getStrengths()))
                .missingConcepts(safe(aiResponse.getMissingConcepts()))
                .misconceptions(safe(aiResponse.getMisconceptions()))
                .feedback(aiResponse.getFeedback() != null ? aiResponse.getFeedback() : "")
                .suggestedNextSteps(safe(aiResponse.getSuggestedNextSteps()))
                .build();
    }

    private List<String> safe(List<String> list) {
        return list == null ? List.of() : list;
    }

    /** 특정 강의의 자기 설명 기록 (최신순) — 본인 것만 */
    public List<SelfExplainHistoryItem> getHistoryByLecture(Long userId, Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        List<LearningLog> logs = learningLogRepository
                .findByUserIdAndLectureIdAndActivityTypeOrderByCreatedAtDesc(userId, lectureId, "SELF_EXPLAIN");

        List<SelfExplainHistoryItem> items = new ArrayList<>();
        for (LearningLog log : logs) {
            items.add(toItem(log, lecture));
        }
        return items;
    }

    /** 사용자의 모든 자기 설명 기록 (최신순) — 회고 페이지용 */
    public List<SelfExplainHistoryItem> getAllHistory(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<LearningLog> logs = learningLogRepository
                .findByUserIdAndActivityTypeOrderByCreatedAtDesc(userId, "SELF_EXPLAIN");

        List<SelfExplainHistoryItem> items = new ArrayList<>();
        for (LearningLog logEntry : logs) {
            items.add(toItem(logEntry, logEntry.getLecture()));
        }
        return items;
    }

    private SelfExplainHistoryItem toItem(LearningLog logEntry, Lecture lecture) {
        Map<String, Object> meta = Collections.emptyMap();
        if (logEntry.getMetadata() != null) {
            try {
                meta = objectMapper.readValue(logEntry.getMetadata(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.warn("자기 설명 metadata 파싱 실패 logId={}: {}", logEntry.getId(), e.getMessage());
            }
        }

        Course course = lecture.getCourse();
        return SelfExplainHistoryItem.builder()
                .id(logEntry.getId())
                .lectureId(lecture.getId())
                .lectureTitle(lecture.getTitle())
                .courseId(course != null ? course.getId() : null)
                .courseTitle(course != null ? course.getTitle() : null)
                .overallScore(asInt(meta.get("overallScore")))
                .grade(asString(meta.get("grade")))
                .focusTopic(asString(meta.get("focusTopic")))
                .explanation(asString(meta.get("explanation")))
                .strengths(asStringList(meta.get("strengths")))
                .missingConcepts(asStringList(meta.get("missingConcepts")))
                .misconceptions(asStringList(meta.get("misconceptions")))
                .createdAt(logEntry.getCreatedAt())
                .build();
    }

    private Integer asInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return null;
    }

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private List<String> asStringList(Object o) {
        if (o instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item != null) out.add(item.toString());
            }
            return out;
        }
        return List.of();
    }
}
