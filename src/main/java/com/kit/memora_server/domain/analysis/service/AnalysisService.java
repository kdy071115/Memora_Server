package com.kit.memora_server.domain.analysis.service;

import com.kit.memora_server.domain.analysis.dto.CourseOverviewResponse;
import com.kit.memora_server.domain.analysis.dto.CourseStudentDetailResponse;
import com.kit.memora_server.domain.analysis.dto.CourseStudentSummary;
import com.kit.memora_server.domain.analysis.dto.MyAnalysisResponse;
import com.kit.memora_server.domain.analysis.dto.StudentDashboardSummary;
import com.kit.memora_server.domain.analysis.dto.StudentDistributionDto;
import com.kit.memora_server.domain.assignment.repository.AssignmentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionRepository;
import com.kit.memora_server.domain.assignment.entity.Assignment;
import com.kit.memora_server.domain.analysis.dto.WeakConceptDto;
import com.kit.memora_server.domain.analysis.dto.WeeklyProgressDto;
import com.kit.memora_server.domain.analysis.entity.LearningLog;
import com.kit.memora_server.domain.analysis.repository.LearningLogRepository;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.entity.Enrollment;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.quiz.entity.QuizAttempt;
import com.kit.memora_server.domain.quiz.repository.QuizAttemptRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.infra.ai.AiServerClient;
import com.kit.memora_server.infra.ai.dto.AiAnalysisRequest;
import com.kit.memora_server.infra.ai.dto.AiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private static final double WEAK_CONCEPT_THRESHOLD = 0.6;
    private static final int MIN_ATTEMPT_COUNT = 2;
    private static final int WEAK_CONCEPT_LIMIT = 5;
    private static final int RECENT_WEEKS = 4;
    private static final int ACTIVE_DAYS = 7;

    // 학생 분류 임계값 (학생별 overallScore 기준)
    private static final int EXCELLENT_THRESHOLD = 85;
    private static final int GOOD_THRESHOLD = 70;
    private static final int AVERAGE_THRESHOLD = 50;

    /** 프론트 레이더 차트가 고정으로 기대하는 6개 역량 키 (순서 유지) */
    private static final List<String> COMPETENCY_KEYS = List.of(
            "개념 이해력", "수학적 사고", "비판적 추론", "암기력", "응용력", "문제 해결"
    );

    private final QuizAttemptRepository quizAttemptRepository;
    private final LearningLogRepository learningLogRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AiServerClient aiServerClient;

    /**
     * 학생 대시보드 상단 요약 카드용 — AI 호출 없이 가벼운 통계만.
     */
    public StudentDashboardSummary getDashboardSummary(Long userId) {
        // 이번 주 (월요일 00:00) 부터의 학습 시간
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDateTime since = monday.atStartOfDay();
        long thisWeek = learningLogRepository.sumDurationByUserIdSince(userId, since);

        long total = learningLogRepository.sumDurationByUserId(userId);

        // 평균 점수 + 정답률
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId);
        int avgScore = (int) attempts.stream()
                .mapToInt(a -> a.getScore() == null ? 0 : a.getScore())
                .average()
                .orElse(0.0);
        int correctRate = attempts.isEmpty() ? 0 : (int) Math.round(
                attempts.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count() * 100.0
                        / attempts.size()
        );

        // 미제출 + 마감 미래인 과제 수
        List<Course> activeCourses = enrollmentRepository.findByUserId(userId).stream()
                .map(Enrollment::getCourse)
                .filter(c -> c != null && "ACTIVE".equals(c.getStatus()))
                .toList();
        long pending = 0;
        if (!activeCourses.isEmpty()) {
            List<Assignment> upcoming = assignmentRepository.findUpcomingByCourses(activeCourses, LocalDateTime.now());
            for (Assignment a : upcoming) {
                boolean mine = submissionRepository.findByAssignmentIdOrderByCreatedAtDesc(a.getId())
                        .stream().anyMatch(s -> s.getSubmitter().getId().equals(userId));
                if (!mine) pending++;
            }
        }

        return StudentDashboardSummary.builder()
                .thisWeekStudyTime(thisWeek)
                .totalStudyTime(total)
                .pendingAssignments(pending)
                .averageScore(avgScore)
                .overallCorrectRate(correctRate)
                .build();
    }

    public MyAnalysisResponse getMyAnalysis(Long userId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId);

        long totalAttempts = attempts.size();
        long correctCount = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        double overallCorrectRate = totalAttempts == 0 ? 0.0 : (double) correctCount / totalAttempts;

        int overallScore = (int) attempts.stream()
                .mapToInt(a -> a.getScore() == null ? 0 : a.getScore())
                .average()
                .orElse(0.0);

        long totalStudyTime = learningLogRepository.sumDurationByUserId(userId);
        long enrolledCourses = enrollmentRepository.findByUserId(userId).size();

        List<WeakConceptDto> weakConcepts = computeWeakConcepts(attempts);
        List<WeeklyProgressDto> weeklyProgress = computeWeeklyProgress(userId, attempts);

        // AI 서버에 진단/추천 요청
        AiAnalysisRequest aiReq = AiAnalysisRequest.builder()
                .correctRate(overallCorrectRate)
                .weakConcepts(weakConcepts.stream().map(WeakConceptDto::getConcept).toList())
                .questionPatterns(List.of())
                .studyTimeTrend(buildStudyTimeTrend(weeklyProgress))
                .build();
        AiAnalysisResponse aiRes = aiServerClient.analyzeLearning(aiReq);

        return MyAnalysisResponse.builder()
                .overallScore(overallScore)
                .totalStudyTime(totalStudyTime)
                .totalQuizAttempts(totalAttempts)
                .overallCorrectRate(round2(overallCorrectRate))
                .enrolledCourses(enrolledCourses)
                .weakConcepts(weakConcepts)
                .diagnosis(aiRes.getDiagnosis())
                .recommendations(aiRes.getRecommendations() != null ? aiRes.getRecommendations() : List.of())
                .motivation(aiRes.getMotivation())
                .weeklyProgress(weeklyProgress)
                .competencies(normalizeCompetencies(aiRes.getCompetencies(), overallScore))
                .maxGrowthIndicator(
                        aiRes.getMaxGrowthIndicator() != null && !aiRes.getMaxGrowthIndicator().isBlank()
                                ? aiRes.getMaxGrowthIndicator()
                                : "꾸준한 학습이 누적되고 있습니다"
                )
                .build();
    }

    /** 교직자용 강의 분석 대시보드 */
    public CourseOverviewResponse getCourseOverview(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        List<Enrollment> enrollments = enrollmentRepository.findWithUserByCourseId(courseId);
        long totalStudents = enrollments.size();

        List<QuizAttempt> allAttempts = new ArrayList<>();
        List<Integer> studentScores = new ArrayList<>();
        long totalStudyTimeSum = 0L;
        long activeStudents = 0L;
        long excellent = 0, good = 0, average = 0, needsHelp = 0;

        LocalDateTime activeSince = LocalDateTime.now().minusDays(ACTIVE_DAYS);

        for (Enrollment e : enrollments) {
            Long studentId = e.getUser().getId();
            List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdAndCourseId(studentId, courseId);
            allAttempts.addAll(attempts);

            int studentScore = computeOverallScore(attempts);
            studentScores.add(studentScore);

            long studyTime = learningLogRepository.sumDurationByUserIdAndCourseId(studentId, courseId);
            totalStudyTimeSum += studyTime;

            LocalDateTime lastActive = findLastActiveAt(studentId, courseId, attempts);
            boolean isActive = lastActive != null && lastActive.isAfter(activeSince);
            if (isActive) {
                activeStudents++;
            }

            String status = classifyStudent(studentScore, lastActive, activeSince);
            switch (status) {
                case "EXCELLENT" -> excellent++;
                case "GOOD" -> good++;
                case "AVERAGE" -> average++;
                default -> needsHelp++;
            }
        }

        long totalAttempts = allAttempts.size();
        long correctCount = allAttempts.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        double averageCorrectRate = totalAttempts == 0 ? 0.0 : (double) correctCount / totalAttempts;

        int averageScore = studentScores.isEmpty()
                ? 0
                : (int) studentScores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        long averageStudyTime = totalStudents == 0 ? 0L : totalStudyTimeSum / totalStudents;

        List<WeakConceptDto> topWeak = computeWeakConcepts(allAttempts);
        Map<String, Integer> competencies = normalizeCompetencies(null, averageScore);
        List<WeeklyProgressDto> weekly = computeCourseWeeklyProgress(courseId, allAttempts);

        return CourseOverviewResponse.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .totalStudents(totalStudents)
                .activeStudents(activeStudents)
                .averageScore(averageScore)
                .averageCorrectRate(round2(averageCorrectRate))
                .averageStudyTime(averageStudyTime)
                .totalQuizAttempts(totalAttempts)
                .topWeakConcepts(topWeak)
                .competencies(competencies)
                .weeklyProgress(weekly)
                .studentDistribution(StudentDistributionDto.builder()
                        .excellent(excellent)
                        .good(good)
                        .average(average)
                        .needsHelp(needsHelp)
                        .build())
                .build();
    }

    /** 교직자용 수강생 목록 */
    public List<CourseStudentSummary> getCourseStudents(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        List<Enrollment> enrollments = enrollmentRepository.findWithUserByCourseId(courseId);
        LocalDateTime activeSince = LocalDateTime.now().minusDays(ACTIVE_DAYS);

        List<CourseStudentSummary> result = new ArrayList<>();
        for (Enrollment e : enrollments) {
            User student = e.getUser();
            result.add(buildStudentSummary(student, courseId, activeSince));
        }
        return result;
    }

    /** 교직자용 수강생 개별 드릴다운 분석 */
    public CourseStudentDetailResponse getCourseStudentDetail(Long courseId, Long studentUserId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (!enrollmentRepository.existsByUserIdAndCourseId(studentUserId, courseId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        User student = enrollmentRepository.findByUserIdAndCourseId(studentUserId, courseId)
                .map(Enrollment::getUser)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdAndCourseId(studentUserId, courseId);
        long totalAttempts = attempts.size();
        long correctCount = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        double correctRate = totalAttempts == 0 ? 0.0 : (double) correctCount / totalAttempts;

        int overallScore = computeOverallScore(attempts);
        long totalStudyTime = learningLogRepository.sumDurationByUserIdAndCourseId(studentUserId, courseId);

        LocalDateTime lastActive = findLastActiveAt(studentUserId, courseId, attempts);
        LocalDateTime activeSince = LocalDateTime.now().minusDays(ACTIVE_DAYS);
        String status = classifyStudent(overallScore, lastActive, activeSince);

        List<WeakConceptDto> weakConcepts = computeWeakConcepts(attempts);
        List<WeeklyProgressDto> weeklyProgress = computeStudentCourseWeeklyProgress(studentUserId, courseId, attempts);
        Map<String, Integer> competencies = normalizeCompetencies(null, overallScore);

        return CourseStudentDetailResponse.builder()
                .userId(student.getId())
                .userName(student.getName())
                .userEmail(student.getEmail())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .overallScore(overallScore)
                .totalStudyTime(totalStudyTime)
                .totalQuizAttempts(totalAttempts)
                .overallCorrectRate(round2(correctRate))
                .lastActiveAt(lastActive)
                .status(status)
                .weakConcepts(weakConcepts)
                .weeklyProgress(weeklyProgress)
                .competencies(competencies)
                .build();
    }

    private CourseStudentSummary buildStudentSummary(User student, Long courseId, LocalDateTime activeSince) {
        Long studentId = student.getId();
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdAndCourseId(studentId, courseId);
        long totalAttempts = attempts.size();
        long correctCount = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        double correctRate = totalAttempts == 0 ? 0.0 : (double) correctCount / totalAttempts;

        int overallScore = computeOverallScore(attempts);
        long studyTime = learningLogRepository.sumDurationByUserIdAndCourseId(studentId, courseId);
        LocalDateTime lastActive = findLastActiveAt(studentId, courseId, attempts);
        String status = classifyStudent(overallScore, lastActive, activeSince);

        return CourseStudentSummary.builder()
                .userId(studentId)
                .name(student.getName())
                .email(student.getEmail())
                .averageScore(overallScore)
                .correctRate(round2(correctRate))
                .totalQuizAttempts(totalAttempts)
                .totalStudyTime(studyTime)
                .lastActiveAt(lastActive)
                .status(status)
                .build();
    }

    private int computeOverallScore(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) return 0;
        return (int) attempts.stream()
                .mapToInt(a -> a.getScore() == null ? 0 : a.getScore())
                .average()
                .orElse(0.0);
    }

    private LocalDateTime findLastActiveAt(Long studentId, Long courseId, List<QuizAttempt> attempts) {
        LocalDateTime lastLearning = learningLogRepository.findMaxCreatedAtByUserIdAndCourseId(studentId, courseId);
        LocalDateTime lastAttempt = attempts.stream()
                .map(QuizAttempt::getAttemptedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (lastLearning == null) return lastAttempt;
        if (lastAttempt == null) return lastLearning;
        return lastLearning.isAfter(lastAttempt) ? lastLearning : lastAttempt;
    }

    private String classifyStudent(int overallScore, LocalDateTime lastActive, LocalDateTime activeSince) {
        boolean inactive = lastActive == null || lastActive.isBefore(activeSince);
        if (inactive) return "NEEDS_HELP";
        if (overallScore >= EXCELLENT_THRESHOLD) return "EXCELLENT";
        if (overallScore >= GOOD_THRESHOLD) return "GOOD";
        if (overallScore >= AVERAGE_THRESHOLD) return "AVERAGE";
        return "NEEDS_HELP";
    }

    /**
     * AI 서버가 내려준 역량 맵을 검증해 고정 6개 키 / 0~150 정수 맵으로 정규화.
     * 키 누락 또는 null 인 경우 overallScore 기반 기본값으로 채운다.
     */
    private Map<String, Integer> normalizeCompetencies(Map<String, Integer> raw, int overallScore) {
        int fallback = Math.max(40, Math.min(140, overallScore == 0 ? 70 : overallScore));
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String key : COMPETENCY_KEYS) {
            Integer value = raw == null ? null : raw.get(key);
            if (value == null) {
                result.put(key, fallback);
            } else {
                result.put(key, Math.max(0, Math.min(150, value)));
            }
        }
        return result;
    }

    private List<WeakConceptDto> computeWeakConcepts(List<QuizAttempt> attempts) {
        Map<String, long[]> agg = new LinkedHashMap<>();
        for (QuizAttempt a : attempts) {
            String tag = a.getQuiz().getConceptTag();
            if (tag == null || tag.isBlank()) continue;
            long[] counts = agg.computeIfAbsent(tag, k -> new long[2]);
            counts[0] += 1;
            if (Boolean.TRUE.equals(a.getIsCorrect())) counts[1] += 1;
        }

        List<WeakConceptDto> result = new ArrayList<>();
        for (Map.Entry<String, long[]> e : agg.entrySet()) {
            long total = e.getValue()[0];
            long correct = e.getValue()[1];
            if (total < MIN_ATTEMPT_COUNT) continue;
            double rate = (double) correct / total;
            if (rate <= WEAK_CONCEPT_THRESHOLD) {
                result.add(WeakConceptDto.builder()
                        .concept(e.getKey())
                        .correctRate(round2(rate))
                        .attemptCount(total)
                        .build());
            }
        }
        result.sort(Comparator.comparingDouble(WeakConceptDto::getCorrectRate));
        return result.size() > WEAK_CONCEPT_LIMIT
                ? result.subList(0, WEAK_CONCEPT_LIMIT)
                : result;
    }

    private List<WeeklyProgressDto> computeWeeklyProgress(Long userId, List<QuizAttempt> attempts) {
        // 최근 4주에 대한 (학습 시간, 평균 점수) 집계
        Map<String, long[]> weekMap = initWeeklyBuckets();

        // 퀴즈 점수 집계
        for (QuizAttempt a : attempts) {
            String key = formatWeek(a.getAttemptedAt());
            long[] v = weekMap.get(key);
            if (v == null) continue;
            v[1] += a.getScore() == null ? 0 : a.getScore();
            v[2] += 1;
        }

        // 학습 시간 집계
        learningLogRepository.findByUserIdOrderByCreatedAtDesc(userId).forEach(l -> {
            String key = formatWeek(l.getCreatedAt());
            long[] v = weekMap.get(key);
            if (v == null) return;
            v[0] += l.getDuration() == null ? 0 : l.getDuration();
        });

        return toWeeklyProgressList(weekMap);
    }

    /** 학급 단위 최근 4주 진행도 — 학생 전체 합계 기반 */
    private List<WeeklyProgressDto> computeCourseWeeklyProgress(Long courseId, List<QuizAttempt> allAttempts) {
        Map<String, long[]> weekMap = initWeeklyBuckets();

        for (QuizAttempt a : allAttempts) {
            String key = formatWeek(a.getAttemptedAt());
            long[] v = weekMap.get(key);
            if (v == null) continue;
            v[1] += a.getScore() == null ? 0 : a.getScore();
            v[2] += 1;
        }

        List<LearningLog> logs = learningLogRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        for (LearningLog l : logs) {
            String key = formatWeek(l.getCreatedAt());
            long[] v = weekMap.get(key);
            if (v == null) continue;
            v[0] += l.getDuration() == null ? 0 : l.getDuration();
        }

        return toWeeklyProgressList(weekMap);
    }

    /** 개별 학생의 강의 범위 주간 진행도 */
    private List<WeeklyProgressDto> computeStudentCourseWeeklyProgress(Long userId, Long courseId, List<QuizAttempt> attempts) {
        Map<String, long[]> weekMap = initWeeklyBuckets();

        for (QuizAttempt a : attempts) {
            String key = formatWeek(a.getAttemptedAt());
            long[] v = weekMap.get(key);
            if (v == null) continue;
            v[1] += a.getScore() == null ? 0 : a.getScore();
            v[2] += 1;
        }

        List<LearningLog> logs = learningLogRepository.findByUserIdAndCourseIdOrderByCreatedAtDesc(userId, courseId);
        for (LearningLog l : logs) {
            String key = formatWeek(l.getCreatedAt());
            long[] v = weekMap.get(key);
            if (v == null) continue;
            v[0] += l.getDuration() == null ? 0 : l.getDuration();
        }

        return toWeeklyProgressList(weekMap);
    }

    private Map<String, long[]> initWeeklyBuckets() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, long[]> weekMap = new LinkedHashMap<>();
        for (int i = RECENT_WEEKS - 1; i >= 0; i--) {
            LocalDateTime weekDate = now.minusWeeks(i);
            String key = formatWeek(weekDate);
            weekMap.put(key, new long[]{0, 0, 0}); // [studyTime, scoreSum, scoreCount]
        }
        return weekMap;
    }

    private List<WeeklyProgressDto> toWeeklyProgressList(Map<String, long[]> weekMap) {
        List<WeeklyProgressDto> result = new ArrayList<>();
        for (Map.Entry<String, long[]> e : weekMap.entrySet()) {
            long[] v = e.getValue();
            Integer score = v[2] == 0 ? null : (int) (v[1] / v[2]);
            result.add(WeeklyProgressDto.builder()
                    .week(e.getKey())
                    .studyTime(v[0])
                    .quizScore(score)
                    .build());
        }
        return result;
    }

    private String formatWeek(LocalDateTime dt) {
        int year = dt.get(IsoFields.WEEK_BASED_YEAR);
        int week = dt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    private String buildStudyTimeTrend(List<WeeklyProgressDto> weekly) {
        if (weekly.isEmpty()) return "(데이터 부족)";
        StringBuilder sb = new StringBuilder();
        for (WeeklyProgressDto w : weekly) {
            sb.append(w.getWeek()).append(": ").append(w.getStudyTime()).append("초");
            if (w.getQuizScore() != null) sb.append(" / 평균 ").append(w.getQuizScore()).append("점");
            sb.append("; ");
        }
        return sb.toString();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
