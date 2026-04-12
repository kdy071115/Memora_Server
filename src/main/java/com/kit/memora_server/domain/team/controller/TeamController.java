package com.kit.memora_server.domain.team.controller;

import com.kit.memora_server.domain.team.dto.TeamInvitationRequest;
import com.kit.memora_server.domain.team.dto.TeamInvitationResponse;
import com.kit.memora_server.domain.team.dto.TeamRequest;
import com.kit.memora_server.domain.team.dto.TeamResponse;
import com.kit.memora_server.domain.team.service.TeamService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Team", description = "팀 + 초대장 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "팀 생성 (수강생)")
    @PostMapping("/api/courses/{courseId}/teams")
    public ResponseEntity<ApiResponse<TeamResponse>> create(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("팀이 생성되었습니다.", teamService.create(user.getId(), courseId, request)));
    }

    @Operation(summary = "강의 내 팀 목록")
    @GetMapping("/api/courses/{courseId}/teams")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> list(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.listByCourse(user.getId(), courseId)));
    }

    @Operation(summary = "팀 상세")
    @GetMapping("/api/teams/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> get(
            @PathVariable Long teamId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.get(user.getId(), teamId)));
    }

    @Operation(summary = "팀 정보 수정 (리더)")
    @PutMapping("/api/teams/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> rename(
            @PathVariable Long teamId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("팀 정보가 수정되었습니다.", teamService.rename(user.getId(), teamId, request)));
    }

    @Operation(summary = "팀 해체 (리더)")
    @DeleteMapping("/api/teams/{teamId}")
    public ResponseEntity<ApiResponse<Void>> disband(
            @PathVariable Long teamId,
            @AuthenticationPrincipal CustomUserDetails user) {
        teamService.disband(user.getId(), teamId);
        return ResponseEntity.ok(ApiResponse.ok("팀이 해체되었습니다."));
    }

    @Operation(summary = "팀 탈퇴 (멤버)")
    @DeleteMapping("/api/teams/{teamId}/members/me")
    public ResponseEntity<ApiResponse<Void>> leave(
            @PathVariable Long teamId,
            @AuthenticationPrincipal CustomUserDetails user) {
        teamService.leave(user.getId(), teamId);
        return ResponseEntity.ok(ApiResponse.ok("팀에서 나갔습니다."));
    }

    // ── Invitations ──

    @Operation(summary = "팀에 초대 (멤버만)")
    @PostMapping("/api/teams/{teamId}/invitations")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> invite(
            @PathVariable Long teamId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody TeamInvitationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("초대장을 보냈습니다.", teamService.invite(user.getId(), teamId, request)));
    }

    @Operation(summary = "내가 받은 PENDING 초대장")
    @GetMapping("/api/me/team-invitations")
    public ResponseEntity<ApiResponse<List<TeamInvitationResponse>>> myInvitations(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.myPendingInvitations(user.getId())));
    }

    @Operation(summary = "초대 수락")
    @PostMapping("/api/team-invitations/{invitationId}/accept")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> accept(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok("초대를 수락했습니다.", teamService.accept(user.getId(), invitationId)));
    }

    @Operation(summary = "초대 거절")
    @PostMapping("/api/team-invitations/{invitationId}/reject")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> reject(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok("초대를 거절했습니다.", teamService.reject(user.getId(), invitationId)));
    }
}
