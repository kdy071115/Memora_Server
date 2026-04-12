package com.kit.memora_server.domain.dailymission.service;

import com.kit.memora_server.domain.analysis.repository.LearningLogRepository;
import com.kit.memora_server.domain.assignment.entity.Assignment;
import com.kit.memora_server.domain.assignment.repository.AssignmentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionRepository;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.entity.Enrollment;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.dailymission.dto.DailyMissionResponse;
import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.lecture.repository.LectureRepository;
import com.kit.memora_server.domain.quiz.entity.QuizAttempt;
import com.kit.memora_server.domain.quiz.repository.QuizAttemptRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.infra.ai.AiServerClient;
import com.kit.memora_server.infra.ai.dto.AiDailyMissionRequest;
import com.kit.memora_server.infra.ai.dto.AiDailyMissionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyMissionService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final LearningLogRepository learningLogRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final LectureRepository lectureRepository;
    private final AiServerClient aiServerClient;

    public DailyMissionResponse getMyDailyMissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ── 1. 학습 컨텍스트 수집 ──
        List<Course> activeCourses = enrollmentRepository.findByUserId(userId).stream()
                .map(Enrollment::getCourse)
                .filter(c -> c != null && "ACTIVE".equals(c.getStatus()))
                .toList();

        // 약점 개념 — 전체 시도에서 틀린 것의 conceptTag 빈도 상위 3개
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId);
        int avgScore = (int) attempts.stream()
                .mapToInt(a -> a.getScore() == null ? 0 : a.getScore())
                .average()
                .orElse(0.0);

        Map<String, Integer> wrong = new HashMap<>();
        for (QuizAttempt a : attempts) {
            if (Boolean.TRUE.equals(a.getIsCorrect())) continue;
            String tag = a.getQuiz() != null ? a.getQuiz().getConceptTag() : null;
            if (tag == null || tag.isBlank()) continue;
            wrong.merge(tag, 1, Integer::sum);
        }
        List<String> weakConcepts = wrong.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        // 미제출 + 마감 미래 과제
        List<String> pendingAssignments = new ArrayList<>();
        if (!activeCourses.isEmpty()) {
            List<Assignment> upcoming = assignmentRepository.findUpcomingByCourses(activeCourses, LocalDateTime.now());
            for (Assignment a : upcoming) {
                boolean mine = submissionRepository.findByAssignmentIdOrderByCreatedAtDesc(a.getId())
                        .stream().anyMatch(s -> s.getSubmitter().getId().equals(userId));
                if (!mine) pendingAssignments.add(a.getTitle());
            }
        }

        // 최근 차시 (각 강의의 가장 최근 차시 1개씩)
        List<String> upcomingLectureTitles = new ArrayList<>();
        for (Course c : activeCourses) {
            List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(c.getId());
            if (!lectures.isEmpty()) {
                upcomingLectureTitles.add(c.getTitle() + " — " + lectures.get(lectures.size() - 1).getTitle());
            }
        }

        // 마지막 학습으로부터 며칠
        Integer daysSinceLastStudy = null;
        LocalDateTime maxLast = null;
        for (Course c : activeCourses) {
            LocalDateTime t = learningLogRepository.findMaxCreatedAtByUserIdAndCourseId(userId, c.getId());
            if (t != null && (maxLast == null || t.isAfter(maxLast))) maxLast = t;
        }
        if (maxLast != null) {
            daysSinceLastStudy = (int) Duration.between(maxLast, LocalDateTime.now()).toDays();
        }

        // ── 2. AI 호출 ──
        AiDailyMissionRequest req = AiDailyMissionRequest.builder()
                .studentName(user.getName())
                .weakConcepts(weakConcepts)
                .pendingAssignments(pendingAssignments)
                .upcomingLectureTitles(upcomingLectureTitles)
                .averageScore(attempts.isEmpty() ? null : avgScore)
                .lastSelfExplainScore(null) // self-explain 점수는 별도 LearningLog metadata 라 이번엔 생략
                .daysSinceLastStudy(daysSinceLastStudy)
                .build();

        AiDailyMissionResponse aiRes;
        try {
            aiRes = aiServerClient.generateDailyMissions(req);
        } catch (Exception e) {
            log.warn("데일리 미션 AI 호출 실패: {}", e.getMessage());
            return fallback(user.getName());
        }

        List<DailyMissionResponse.MissionItem> items = new ArrayList<>();
        if (aiRes.getMissions() != null) {
            for (AiDailyMissionResponse.MissionItem m : aiRes.getMissions()) {
                items.add(DailyMissionResponse.MissionItem.builder()
                        .type(m.getType())
                        .title(m.getTitle())
                        .description(m.getDescription())
                        .estimatedMinutes(m.getEstimatedMinutes())
                        .why(m.getWhy())
                        .build());
            }
        }

        return DailyMissionResponse.builder()
                .summary(aiRes.getSummary() != null ? aiRes.getSummary() : "")
                .motivation(aiRes.getMotivation() != null ? aiRes.getMotivation() : "")
                .missions(items)
                .build();
    }

    private DailyMissionResponse fallback(String name) {
        return DailyMissionResponse.builder()
                .summary("오늘은 어제 배운 내용을 한 번 더 정리해보세요")
                .motivation("작은 한 걸음이 큰 차이를 만들어요!")
                .missions(List.of(
                        DailyMissionResponse.MissionItem.builder()
                                .type("REVIEW")
                                .title("어제 학습 내용 5분 회고")
                                .description("가장 최근 차시 자료를 한 번 훑어보고, 가장 인상 깊었던 부분 한 가지를 떠올려보세요.")
                                .estimatedMinutes(5)
                                .why("짧은 회고가 장기 기억에 가장 효과적이에요")
                                .build()
                ))
                .build();
    }
}
