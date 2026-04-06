package com.kit.memora_server.domain.analysis.service;

import com.kit.memora_server.domain.analysis.dto.MyAnalysisResponse;
import com.kit.memora_server.domain.analysis.dto.WeakConceptDto;
import com.kit.memora_server.domain.analysis.dto.WeeklyProgressDto;
import com.kit.memora_server.domain.analysis.repository.LearningLogRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.quiz.entity.QuizAttempt;
import com.kit.memora_server.domain.quiz.repository.QuizAttemptRepository;
import com.kit.memora_server.infra.ai.AiServerClient;
import com.kit.memora_server.infra.ai.dto.AiAnalysisRequest;
import com.kit.memora_server.infra.ai.dto.AiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final QuizAttemptRepository quizAttemptRepository;
    private final LearningLogRepository learningLogRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AiServerClient aiServerClient;

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
                .build();
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
        LocalDateTime now = LocalDateTime.now();
        Map<String, long[]> weekMap = new LinkedHashMap<>();

        for (int i = RECENT_WEEKS - 1; i >= 0; i--) {
            LocalDateTime weekDate = now.minusWeeks(i);
            String key = formatWeek(weekDate);
            weekMap.put(key, new long[]{0, 0, 0}); // [studyTime, scoreSum, scoreCount]
        }

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
