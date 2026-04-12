package com.kit.memora_server.domain.team.service;

import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.course.repository.CourseRepository;
import com.kit.memora_server.domain.course.repository.EnrollmentRepository;
import com.kit.memora_server.domain.team.dto.TeamInvitationRequest;
import com.kit.memora_server.domain.team.dto.TeamInvitationResponse;
import com.kit.memora_server.domain.team.dto.TeamMemberDto;
import com.kit.memora_server.domain.team.dto.TeamRequest;
import com.kit.memora_server.domain.team.dto.TeamResponse;
import com.kit.memora_server.domain.team.entity.InvitationStatus;
import com.kit.memora_server.domain.team.entity.Team;
import com.kit.memora_server.domain.team.entity.TeamInvitation;
import com.kit.memora_server.domain.team.entity.TeamMember;
import com.kit.memora_server.domain.team.repository.TeamInvitationRepository;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamResponse create(Long userId, Long courseId, TeamRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        Team team = Team.builder()
                .course(course)
                .leader(user)
                .name(request.getName())
                .description(request.getDescription())
                .build();
        teamRepository.save(team);

        TeamMember leaderMember = TeamMember.builder().team(team).user(user).build();
        teamMemberRepository.save(leaderMember);

        return TeamResponse.from(team, List.of(TeamMemberDto.from(leaderMember)));
    }

    public List<TeamResponse> listByCourse(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        boolean isInstructor = course.getInstructor().getId().equals(userId);
        if (!isInstructor && !enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        return teamRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(team -> {
                    List<TeamMemberDto> members = teamMemberRepository.findByTeamIdWithUser(team.getId())
                            .stream().map(TeamMemberDto::from).toList();
                    return TeamResponse.from(team, members);
                })
                .toList();
    }

    public TeamResponse get(Long userId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        Course course = team.getCourse();
        boolean isInstructor = course.getInstructor().getId().equals(userId);
        if (!isInstructor && !enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId())) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }
        List<TeamMemberDto> members = teamMemberRepository.findByTeamIdWithUser(teamId)
                .stream().map(TeamMemberDto::from).toList();
        return TeamResponse.from(team, members);
    }

    @Transactional
    public TeamResponse rename(Long userId, Long teamId, TeamRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        if (!team.getLeader().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        team.rename(request.getName(), request.getDescription());
        List<TeamMemberDto> members = teamMemberRepository.findByTeamIdWithUser(teamId)
                .stream().map(TeamMemberDto::from).toList();
        return TeamResponse.from(team, members);
    }

    @Transactional
    public void disband(Long userId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        if (!team.getLeader().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        teamInvitationRepository.deleteByTeamId(teamId);
        teamMemberRepository.deleteByTeamId(teamId);
        teamRepository.delete(team);
    }

    @Transactional
    public void leave(Long userId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        if (team.getLeader().getId().equals(userId)) {
            // 리더는 떠날 수 없음 — 해체해야 함
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new BusinessException(ErrorCode.NOT_TEAM_MEMBER);
        }
        teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId);
    }

    // ── Invitations ──

    @Transactional
    public TeamInvitationResponse invite(Long userId, Long teamId, TeamInvitationRequest request) {
        User inviter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 멤버만 초대 가능
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new BusinessException(ErrorCode.NOT_TEAM_MEMBER);
        }

        Long inviteeId = request.getInviteeId();
        if (inviteeId.equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        User invitee = userRepository.findById(inviteeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 같은 강의 수강생만 초대 가능
        Long courseId = team.getCourse().getId();
        if (!enrollmentRepository.existsByUserIdAndCourseId(inviteeId, courseId)) {
            throw new BusinessException(ErrorCode.NOT_ENROLLED);
        }

        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, inviteeId)) {
            throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
        }
        if (teamInvitationRepository.existsByTeamIdAndInviteeIdAndStatus(teamId, inviteeId, InvitationStatus.PENDING)) {
            throw new BusinessException(ErrorCode.ALREADY_INVITED);
        }

        TeamInvitation invitation = TeamInvitation.builder()
                .team(team)
                .inviter(inviter)
                .invitee(invitee)
                .build();
        teamInvitationRepository.save(invitation);
        return TeamInvitationResponse.from(invitation);
    }

    public List<TeamInvitationResponse> myPendingInvitations(Long userId) {
        return teamInvitationRepository
                .findByInviteeIdAndStatusOrderByCreatedAtDesc(userId, InvitationStatus.PENDING)
                .stream()
                .map(TeamInvitationResponse::from)
                .toList();
    }

    @Transactional
    public TeamInvitationResponse accept(Long userId, Long invitationId) {
        TeamInvitation inv = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
        if (!inv.getInvitee().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_HANDLED);
        }

        // 이미 멤버라면 단순히 상태만 ACCEPTED 로
        if (!teamMemberRepository.existsByTeamIdAndUserId(inv.getTeam().getId(), userId)) {
            TeamMember member = TeamMember.builder()
                    .team(inv.getTeam())
                    .user(inv.getInvitee())
                    .build();
            teamMemberRepository.save(member);
        }
        inv.accept();
        return TeamInvitationResponse.from(inv);
    }

    @Transactional
    public TeamInvitationResponse reject(Long userId, Long invitationId) {
        TeamInvitation inv = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
        if (!inv.getInvitee().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_HANDLED);
        }
        inv.reject();
        return TeamInvitationResponse.from(inv);
    }
}
