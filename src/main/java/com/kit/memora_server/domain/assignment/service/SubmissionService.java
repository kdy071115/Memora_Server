package com.kit.memora_server.domain.assignment.service;

import com.kit.memora_server.domain.assignment.dto.SubmissionCommentRequest;
import com.kit.memora_server.domain.assignment.dto.SubmissionCommentResponse;
import com.kit.memora_server.domain.assignment.dto.SubmissionRequest;
import com.kit.memora_server.domain.assignment.dto.SubmissionResponse;
import com.kit.memora_server.domain.assignment.entity.Assignment;
import com.kit.memora_server.domain.assignment.entity.Submission;
import com.kit.memora_server.domain.assignment.entity.SubmissionComment;
import com.kit.memora_server.domain.assignment.entity.SubmissionVisibility;
import com.kit.memora_server.domain.assignment.repository.AssignmentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionCommentRepository;
import com.kit.memora_server.domain.assignment.repository.SubmissionRepository;
import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.document.service.S3Service;
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
