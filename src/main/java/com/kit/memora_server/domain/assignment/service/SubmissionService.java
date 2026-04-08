package com.kit.memora_server.domain.assignment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kit.memora_server.domain.assignment.dto.AiFeedbackResponse;
import com.kit.memora_server.domain.assignment.dto.SubmissionCommentRequest;
import com.kit.memora_server.domain.assignment.dto.SubmissionCommentResponse;
import com.kit.memora_server.domain.assignment.dto.SubmissionRequest;
import com.kit.memora_server.domain.assignment.dto.SubmissionResponse;
import com.kit.memora_server.domain.assignment.entity.AiSubmissionFeedback;
import com.kit.memora_server.domain.assignment.entity.Assignment;
import com.kit.memora_server.domain.assignment.entity.Submission;
import com.kit.memora_server.domain.assignment.entity.SubmissionComment;
import com.kit.memora_server.domain.assignment.entity.SubmissionVisibility;
import com.kit.memora_server.domain.assignment.repository.AiSubmissionFeedbackRepository;
import com.kit.memora_server.domain.assignment.repository.AssignmentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionCommentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionRepository;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.document.service.S3Service;
import com.kit.memora_server.infra.ai.AiServerClient;
import com.kit.memora_server.infra.ai.dto.AiAssignmentFeedbackRequest;
import com.kit.memora_server.infra.ai.dto.AiAssignmentFeedbackResponse;
import com.kit.memora_server.domain.team.entity.Team;
import com.kit.memora_server.domain.team.repository.TeamMemberRepository;
import com.kit.memora_server.domain.team.repository.TeamRepository;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.domain.user.repository.UserRepository;
import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionCommentRepository submissionCommentRepository;
    private final AssignmentRepository assignmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final AiServerClient aiServerClient;
    private final AiSubmissionFeedbackRepository aiSubmissionFeedbackRepository;
    private final ObjectMapper objectMapper;

    /** AI 피드백 생성 시 base64 인코딩할 첨부 최대 크기 (10MB) */
    private static final long MAX_AI_ATTACHMENT_BYTES = 10L * 1024 * 1024;

    @Transactional
    public SubmissionResponse create(Long userId, Long assignmentId, SubmissionRequest request, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        Course course = assignment.getCourse();
        // 강사는 자기 강의 외 과제에 제출하지 않는다고 가정 — 하지만 막을 필요는 없음.
        // 학생은 수강 등록 필수.
        if (!course.getInstructor().getId().equals(userId)) {
            if (!enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId())) {
                throw new BusinessException(ErrorCode.NOT_ENROLLED);
            }
        }

        Team team = resolveTeam(assignment, request.getTeamId(), userId);

        Submission submission = Submission.builder()
                .assignment(assignment)
                .submitter(user)
                .team(team)
                .content(request.getContent())
                .visibility(request.getVisibility() != null ? request.getVisibility() : SubmissionVisibility.PRIVATE)
                .build();

        if (file != null && !file.isEmpty()) {
            String stored = s3Service.upload(file, "submissions/" + assignmentId);
            submission.replaceAttachment(stored, file.getOriginalFilename(), file.getSize());
        }

        submissionRepository.save(submission);
        return SubmissionResponse.from(submission, null);
    }

    public List<SubmissionResponse> listVisible(Long userId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        Course course = assignment.getCourse();
        boolean isInstructor = course.getInstructor().getId().equals(userId);
        if (!isInstructor) {
            if (!enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId())) {
                throw new BusinessException(ErrorCode.NOT_ENROLLED);
            }
        }

        List<Long> myTeamIds = teamMemberRepository.findByUserId(userId).stream()
                .map(m -> m.getTeam().getId()).toList();

        return submissionRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId).stream()
                .filter(s -> canSee(s, userId, isInstructor, myTeamIds))
                .map(s -> SubmissionResponse.from(s, null))
                .toList();
    }

    public SubmissionResponse get(Long userId, Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        Course course = submission.getAssignment().getCourse();
        boolean isInstructor = course.getInstructor().getId().equals(userId);
        if (!isInstructor) {
            if (!enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId())) {
                throw new BusinessException(ErrorCode.NOT_ENROLLED);
            }
        }

        List<Long> myTeamIds = teamMemberRepository.findByUserId(userId).stream()
                .map(m -> m.getTeam().getId()).toList();
        if (!canSee(submission, userId, isInstructor, myTeamIds)) {
            throw new BusinessException(ErrorCode.SUBMISSION_FORBIDDEN);
        }

        List<SubmissionCommentResponse> comments = submissionCommentRepository
                .findBySubmissionIdOrderByCreatedAtAsc(submissionId).stream()
                .map(SubmissionCommentResponse::from)
                .toList();

        return SubmissionResponse.from(submission, comments);
    }

    @Transactional
    public SubmissionResponse update(Long userId, Long submissionId, SubmissionRequest request, MultipartFile file, boolean removeAttachment) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        // 본인만 수정
        if (!submission.getSubmitter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Team team = resolveTeam(submission.getAssignment(), request.getTeamId(), userId);
        submission.update(request.getContent(), request.getVisibility(), team);

        if (removeAttachment) submission.clearAttachment();
        if (file != null && !file.isEmpty()) {
            String stored = s3Service.upload(file, "submissions/" + submission.getAssignment().getId());
            submission.replaceAttachment(stored, file.getOriginalFilename(), file.getSize());
        }

        // 본문/첨부가 바뀌었으므로 캐시된 AI 피드백은 더 이상 유효하지 않음
        aiSubmissionFeedbackRepository.deleteBySubmissionId(submissionId);

        return SubmissionResponse.from(submission, null);
    }

    @Transactional
    public void delete(Long userId, Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        boolean isOwner = submission.getSubmitter().getId().equals(userId);
        boolean isInstructor = submission.getAssignment().getCourse().getInstructor().getId().equals(userId);
        if (!isOwner && !isInstructor) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        aiSubmissionFeedbackRepository.deleteBySubmissionId(submissionId);
        submissionCommentRepository.deleteBySubmissionId(submissionId);
        submissionRepository.delete(submission);
    }

    // ── Comments ──

    @Transactional
    public SubmissionCommentResponse addComment(Long userId, Long submissionId, SubmissionCommentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        Course course = submission.getAssignment().getCourse();
        boolean isInstructor = course.getInstructor().getId().equals(userId);
        boolean isOwner = submission.getSubmitter().getId().equals(userId);
        // 본인 + 강사만 댓글 가능
        if (!isInstructor && !isOwner) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        SubmissionComment comment = SubmissionComment.builder()
                .submission(submission)
                .author(user)
                .content(request.getContent())
                .build();
        submissionCommentRepository.save(comment);
        return SubmissionCommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        SubmissionComment comment = submissionCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        boolean isAuthor = comment.getAuthor().getId().equals(userId);
        boolean isInstructor = comment.getSubmission().getAssignment().getCourse().getInstructor().getId().equals(userId);
        if (!isAuthor && !isInstructor) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        submissionCommentRepository.delete(comment);
    }

    // ── Helpers ──

    private boolean canSee(Submission s, Long userId, boolean isInstructor, List<Long> myTeamIds) {
        if (isInstructor) return true;
        if (s.getSubmitter().getId().equals(userId)) return true;
        if (s.getTeam() != null && myTeamIds.contains(s.getTeam().getId())) return true;
        return s.getVisibility() == SubmissionVisibility.PUBLIC;
    }

    /**
     * 팀 ID 가 주어진 경우 —
     *  - 과제가 팀 제출 허용해야 하고
     *  - 팀이 같은 강의 소속이어야 하고
     *  - 사용자가 해당 팀의 멤버여야 한다
     */
    private Team resolveTeam(Assignment assignment, Long teamId, Long userId) {
        if (teamId == null) return null;
        if (!assignment.isAllowTeamSubmission()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        if (!team.getCourse().getId().equals(assignment.getCourse().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new BusinessException(ErrorCode.NOT_TEAM_MEMBER);
        }
        return team;
    }

    /**
     * 강사가 학생 제출물에 대한 AI 피드백 초안을 요청.
     * 첨부 파일이 있으면 디스크/S3 에서 읽어 base64 로 AI 서버에 함께 전달.
     * 결과는 저장하지 않고 그대로 반환 — 강사가 검토 후 직접 댓글로 게시한다.
     */
    public AiFeedbackResponse requestAiFeedback(Long userId, Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        // 강사 권한 + 본인 강의 검증
        Course course = submission.getAssignment().getCourse();
        if (!course.getInstructor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String attachmentBase64 = null;
        String attachmentName = submission.getAttachmentName();
        if (submission.getAttachmentPath() != null) {
            Long size = submission.getAttachmentSize();
            if (size != null && size > MAX_AI_ATTACHMENT_BYTES) {
                log.warn("AI 피드백 — 첨부 크기 초과로 본문 생략 size={} max={}", size, MAX_AI_ATTACHMENT_BYTES);
            } else {
                try {
                    byte[] bytes = s3Service.readBytes(submission.getAttachmentPath());
                    if (bytes.length <= MAX_AI_ATTACHMENT_BYTES) {
                        attachmentBase64 = Base64.getEncoder().encodeToString(bytes);
                    }
                } catch (Exception e) {
                    log.warn("AI 피드백 — 첨부 읽기 실패, 본문만 사용: {}", e.getMessage());
                }
            }
        }

        AiAssignmentFeedbackRequest aiRequest = AiAssignmentFeedbackRequest.builder()
                .assignmentTitle(submission.getAssignment().getTitle())
                .assignmentDescription(submission.getAssignment().getDescription())
                .studentName(submission.getSubmitter().getName())
                .submissionContent(submission.getContent())
                .attachmentName(attachmentName)
                .attachmentBase64(attachmentBase64)
                .build();

        AiAssignmentFeedbackResponse aiResponse = aiServerClient.generateAssignmentFeedback(aiRequest);

        AiFeedbackResponse result = AiFeedbackResponse.builder()
                .overallScore(aiResponse.getOverallScore())
                .grade(aiResponse.getGrade())
                .summary(aiResponse.getSummary())
                .strengths(safeList(aiResponse.getStrengths()))
                .improvements(safeList(aiResponse.getImprovements()))
                .missingPoints(safeList(aiResponse.getMissingPoints()))
                .suggestions(safeList(aiResponse.getSuggestions()))
                .instructorDraft(aiResponse.getInstructorDraft() != null ? aiResponse.getInstructorDraft() : "")
                .build();

        // 강사 전용 캐시 upsert — 학생에게는 절대 노출되지 않는다
        try {
            String json = objectMapper.writeValueAsString(result);
            aiSubmissionFeedbackRepository.findBySubmissionId(submissionId)
                    .ifPresentOrElse(
                            existing -> existing.updatePayload(json),
                            () -> aiSubmissionFeedbackRepository.save(
                                    AiSubmissionFeedback.builder()
                                            .submission(submission)
                                            .payloadJson(json)
                                            .build()
                            )
                    );
        } catch (JsonProcessingException e) {
            log.warn("AI 피드백 캐시 직렬화 실패 (저장 스킵) submissionId={}: {}", submissionId, e.getMessage());
        }

        return result;
    }

    /**
     * 강사가 같은 제출물에 대해 이전에 생성한 AI 피드백 캐시를 조회.
     * 없으면 null 반환. 학생/타인은 절대 호출 불가 — 강사 본인 강의만 허용.
     */
    public AiFeedbackResponse getCachedAiFeedback(Long userId, Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        if (!submission.getAssignment().getCourse().getInstructor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return aiSubmissionFeedbackRepository.findBySubmissionId(submissionId)
                .map(cache -> {
                    try {
                        return objectMapper.readValue(cache.getPayloadJson(), AiFeedbackResponse.class);
                    } catch (JsonProcessingException e) {
                        log.warn("AI 피드백 캐시 역직렬화 실패 submissionId={}: {}", submissionId, e.getMessage());
                        return null;
                    }
                })
                .orElse(null);
    }

    private static List<String> safeList(List<String> list) {
        return list == null ? List.of() : list;
    }

    public Submission rawForDownload(Long userId, Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        Course course = submission.getAssignment().getCourse();
        boolean isInstructor = course.getInstructor().getId().equals(userId);
        if (!isInstructor && !enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId())) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        List<Long> myTeamIds = teamMemberRepository.findByUserId(userId).stream()
                .map(m -> m.getTeam().getId()).toList();
        if (!canSee(submission, userId, isInstructor, myTeamIds)) {
            throw new BusinessException(ErrorCode.SUBMISSION_FORBIDDEN);
        }
        return submission;
    }
}
