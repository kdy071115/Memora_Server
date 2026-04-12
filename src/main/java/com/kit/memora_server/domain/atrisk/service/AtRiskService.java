package com.kit.memora_server.domain.atrisk.service;

import com.kit.memora_server.domain.analysis.repository.LearningLogRepository;
import com.kit.memora_server.domain.assignment.repository.AssignmentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionRepository;
import com.kit.memora_server.domain.atrisk.dto.AtRiskStudentItem;
import com.kit.memora_server.domain.atrisk.dto.CareMessageResponse;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.entity.Enrollment;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.quiz.entity.QuizAttempt;
import com.kit.memora_server.domain.quiz.repository.QuizAttemptRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.infra.ai.AiServerClient;
import com.kit.memora_server.infra.ai.dto.AiCareMessageRequest;
import com.kit.memora_server.infra.ai.dto.AiCareMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 강사용 — 강의의 위험 학생 진단 + 케어 메시지 생성.
 *
 * 위험 점수(0~100) 가중치:
 *  - 학습 활동 부재: 5일 이상 → +30
 *  - 정답률 저조: <60% → +25, <40% → +35
 *  - 평균 점수 저조: <50점 → +20, <30점 → +30
 *  - 미제출 과제: 1건 +10, 2건 이상 +20
 *  - 자기 설명 / 활동 자체 없음: +20
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtRiskService {

    private static final int HIGH_THRESHOLD = 60;
    private static final int MEDIUM_THRESHOLD = 35;
    private static final int INACTIVE_DAYS_THRESHOLD = 5;

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final LearningLogRepository learningLogRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AiServerClient aiServerClient;

    /** 강의 단위 위험 학생 진단. instructor 권한 필요 (controller 측 검증). */
    public List<AtRiskStudentItem> getAtRiskByCourse(Long instructorUserId, Long courseId, int limit) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.getInstructor().getId().equals(instructorUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        List<Enrollment> enrollments = enrollmentRepository.findWithUserByCourseId(courseId);
        long totalAssignments = assignmentRepository.countByCourseId(courseId);

        List<AtRiskStudentItem> items = new ArrayList<>();
        for (Enrollment e : enrollments) {
            User u = e.getUser();
            items.add(diagnose(u, course, totalAssignments));
        }

        // 위험 점수 내림차순 + MEDIUM/HIGH 만 노출
        return items.stream()
                .filter(i -> i.getRiskScore() >= MEDIUM_THRESHOLD)
                .sorted(Comparator.comparingInt(AtRiskStudentItem::getRiskScore).reversed())
                .limit(limit)
                .toList();
    }

    private AtRiskStudentItem diagnose(User user, Course course, long totalAssignments) {
        Long userId = user.getId();
        Long courseId = course.getId();

        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdAndCourseId(userId, courseId);
        int avgScore = (int) attempts.stream()
                .mapToInt(a -> a.getScore() == null ? 0 : a.getScore())
                .average()
                .orElse(0.0);
        int correctRate = attempts.isEmpty() ? 0 :
                (int) Math.round(attempts.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count() * 100.0 / attempts.size());

        long studyTime = learningLogRepository.sumDurationByUserIdAndCourseId(userId, courseId);
        LocalDateTime lastActive = learningLogRepository.findMaxCreatedAtByUserIdAndCourseId(userId, courseId);
        Integer daysSince = null;
        if (lastActive != null) {
            daysSince = (int) Duration.between(lastActive, LocalDateTime.now()).toDays();
        }

        // 미제출 과제 수
        long submitted = submissionRepository.countDistinctSubmittedAssignmentsByUserIdAndCourseId(userId, courseId);
        long pending = Math.max(0, totalAssignments - submitted);

        // 약점 개념 (틀린 문제의 conceptTag 빈도 상위 3개)
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

        // 위험 점수 계산
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (lastActive == null) {
            score += 25;
            reasons.add("강의 학습 활동 기록이 없음");
        } else if (daysSince != null && daysSince >= INACTIVE_DAYS_THRESHOLD) {
            score += 30;
            reasons.add(daysSince + "일 동안 학습 활동 없음");
        }

        if (!attempts.isEmpty()) {
            if (correctRate < 40) {
                score += 35;
                reasons.add("퀴즈 정답률 " + correctRate + "% (40% 미만)");
            } else if (correctRate < 60) {
                score += 25;
                reasons.add("퀴즈 정답률 " + correctRate + "% (60% 미만)");
            }

            if (avgScore < 30) {
                score += 30;
                reasons.add("평균 점수 " + avgScore + "점 (30점 미만)");
            } else if (avgScore < 50) {
                score += 20;
                reasons.add("평균 점수 " + avgScore + "점 (50점 미만)");
            }
        } else if (totalAssignments > 0 || studyTime == 0) {
            // 시도 자체가 없으면 약한 시그널
            score += 15;
            reasons.add("아직 퀴즈를 한 번도 풀지 않음");
        }

        if (totalAssignments > 0) {
            if (pending >= 2) {
                score += 20;
                reasons.add("미제출 과제 " + pending + "건");
            } else if (pending == 1) {
                score += 10;
                reasons.add("미제출 과제 1건");
            }
        }

        if (score > 100) score = 100;

        String level;
        if (score >= HIGH_THRESHOLD) level = "HIGH";
        else if (score >= MEDIUM_THRESHOLD) level = "MEDIUM";
        else level = "LOW";

        return AtRiskStudentItem.builder()
                .userId(userId)
                .name(user.getName())
                .email(user.getEmail())
                .riskScore(score)
                .riskLevel(level)
                .reasons(reasons)
                .averageScore(avgScore)
                .correctRatePercent(correctRate)
                .totalStudyTimeSeconds(studyTime)
                .lastActiveAt(lastActive)
                .daysSinceLastActive(daysSince)
                .pendingAssignments(pending)
                .weakConcepts(weakConcepts)
                .build();
    }

    /**
     * 강사가 특정 위험 학생에 대한 케어 메시지 초안을 AI 로 생성.
     * 메시지 자체는 저장하지 않고 그대로 반환 — 강사가 검토해 직접 보낼 수단(이메일/메신저) 으로 활용.
     */
    public CareMessageResponse generateCareMessage(Long instructorUserId, Long courseId, Long studentUserId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.getInstructor().getId().equals(instructorUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        User student = userRepository.findById(studentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!enrollmentRepository.existsByUserIdAndCourseId(studentUserId, courseId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        long totalAssignments = assignmentRepository.countByCourseId(courseId);
        AtRiskStudentItem diag = diagnose(student, course, totalAssignments);

        AiCareMessageRequest req = AiCareMessageRequest.builder()
                .studentName(student.getName())
                .courseTitle(course.getTitle())
                .instructorName(course.getInstructor().getName())
                .riskReasons(diag.getReasons())
                .weakConcepts(diag.getWeakConcepts())
                .daysSinceLastActive(diag.getDaysSinceLastActive())
                .averageScore(diag.getAverageScore())
                .build();

        AiCareMessageResponse aiRes = aiServerClient.generateCareMessage(req);
        return CareMessageResponse.builder()
                .message(aiRes.getMessage() != null ? aiRes.getMessage() : "")
                .suggestedActions(aiRes.getSuggestedActions() != null ? aiRes.getSuggestedActions() : List.of())
                .build();
    }
}
