package com.kit.memora_server.domain.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.lecture.repository.LectureRepository;
import com.kit.memora_server.domain.quiz.dto.QuizAttemptResponse;
import com.kit.memora_server.domain.quiz.dto.QuizCreateRequest;
import com.kit.memora_server.domain.quiz.dto.QuizGenerateRequest;
import com.kit.memora_server.domain.quiz.dto.QuizResponse;
import com.kit.memora_server.domain.quiz.dto.QuizSubmitRequest;
import com.kit.memora_server.domain.quiz.dto.QuizSubmitResponse;
import com.kit.memora_server.domain.quiz.dto.QuizUpdateRequest;
import com.kit.memora_server.domain.quiz.entity.Quiz;
import com.kit.memora_server.domain.quiz.entity.QuizAttempt;
import com.kit.memora_server.domain.quiz.repository.QuizAttemptRepository;
import com.kit.memora_server.domain.quiz.repository.QuizRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import com.kit.memora_server.infra.ai.AiServerClient;
import com.kit.memora_server.infra.ai.dto.AiQuizGenerateRequest;
import com.kit.memora_server.infra.ai.dto.AiQuizGenerateResponse;
import com.kit.memora_server.infra.ai.dto.AiQuizGradeRequest;
import com.kit.memora_server.infra.ai.dto.AiQuizGradeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final AiServerClient aiServerClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<QuizResponse> generate(Long lectureId, QuizGenerateRequest request) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        AiQuizGenerateRequest aiRequest = AiQuizGenerateRequest.builder()
                .lectureId(lectureId)
                .count(request.getCount())
                .types(request.getTypes())
                .difficulty(request.getDifficulty())
                .conceptTags(request.getConceptTags())
                .build();
        AiQuizGenerateResponse aiResponse = aiServerClient.generateQuiz(aiRequest);

        if (aiResponse == null || aiResponse.getQuizzes() == null || aiResponse.getQuizzes().isEmpty()) {
            return List.of();
        }

        List<Quiz> savedQuizzes = new ArrayList<>();
        for (AiQuizGenerateResponse.GeneratedQuiz g : aiResponse.getQuizzes()) {
            String optionsJson = null;
            if (g.getOptions() != null && !g.getOptions().isEmpty()) {
                try {
                    optionsJson = objectMapper.writeValueAsString(g.getOptions());
                } catch (JsonProcessingException e) {
                    log.warn("options 직렬화 실패: {}", e.getMessage());
                }
            }
            Quiz quiz = Quiz.builder()
                    .lecture(lecture)
                    .question(g.getQuestion())
                    .quizType(g.getQuizType())
                    .options(optionsJson)
                    .correctAnswer(g.getCorrectAnswer() == null ? "" : g.getCorrectAnswer())
                    .explanation(g.getExplanation())
                    .difficulty(g.getDifficulty() != null ? g.getDifficulty() : request.getDifficulty())
                    .conceptTag(g.getConceptTag())
                    .build();
            savedQuizzes.add(quizRepository.save(quiz));
        }

        return savedQuizzes.stream()
                .map(q -> QuizResponse.from(q, objectMapper))
                .toList();
    }

    public List<QuizResponse> getQuizzesByLecture(Long lectureId, String difficulty) {
        List<Quiz> quizzes = (difficulty != null && !difficulty.isBlank())
                ? quizRepository.findByLectureIdAndDifficulty(lectureId, difficulty)
                : quizRepository.findByLectureId(lectureId);
        return quizzes.stream()
                .map(q -> QuizResponse.from(q, objectMapper))
                .toList();
    }

    @Transactional
    public QuizSubmitResponse submit(Long quizId, Long userId, QuizSubmitRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        AiQuizGradeRequest gradeRequest = AiQuizGradeRequest.builder()
                .question(quiz.getQuestion())
                .quizType(quiz.getQuizType())
                .correctAnswer(quiz.getCorrectAnswer())
                .userAnswer(request.getUserAnswer())
                .build();
        AiQuizGradeResponse gradeResponse = aiServerClient.gradeQuiz(gradeRequest);

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .user(user)
                .userAnswer(request.getUserAnswer())
                .isCorrect(gradeResponse.getIsCorrect() != null ? gradeResponse.getIsCorrect() : false)
                .score(gradeResponse.getScore() != null ? gradeResponse.getScore() : 0)
                .aiFeedback(gradeResponse.getFeedback())
                .timeSpent(request.getTimeSpent())
                .build();
        QuizAttempt saved = quizAttemptRepository.save(attempt);

        return QuizSubmitResponse.from(saved);
    }

    public List<QuizAttemptResponse> getMyAttempts(Long userId, Long lectureId) {
        List<QuizAttempt> attempts = quizAttemptRepository
                .findByUserIdAndQuiz_LectureIdOrderByAttemptedAtDesc(userId, lectureId);
        return attempts.stream().map(QuizAttemptResponse::from).toList();
    }

    @Transactional
    public QuizResponse create(Long lectureId, Long instructorId, QuizCreateRequest request) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        if (!lecture.getCourse().getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Quiz quiz = Quiz.builder()
                .lecture(lecture)
                .question(request.getQuestion())
                .quizType(request.getQuizType())
                .options(serializeOptions(request.getOptions()))
                .correctAnswer(request.getCorrectAnswer())
                .explanation(request.getExplanation())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : "MEDIUM")
                .conceptTag(request.getConceptTag())
                .build();

        Quiz saved = quizRepository.save(quiz);
        return QuizResponse.from(saved, objectMapper);
    }

    @Transactional
    public QuizResponse update(Long quizId, Long instructorId, QuizUpdateRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        if (!quiz.getLecture().getCourse().getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        quiz.update(
                request.getQuestion(),
                request.getQuizType(),
                serializeOptions(request.getOptions()),
                request.getCorrectAnswer(),
                request.getExplanation(),
                request.getDifficulty(),
                request.getConceptTag()
        );

        return QuizResponse.from(quiz, objectMapper);
    }

    @Transactional
    public void delete(Long quizId, Long instructorId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        if (!quiz.getLecture().getCourse().getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (quizAttemptRepository.existsByQuizId(quizId)) {
            throw new BusinessException(ErrorCode.QUIZ_HAS_ATTEMPTS);
        }

        quizRepository.delete(quiz);
    }

    private String serializeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            log.warn("options 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
