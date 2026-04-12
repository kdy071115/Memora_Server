package com.kit.memora_server.domain.auth.controller;

import com.kit.memora_server.domain.auth.dto.*;
import com.kit.memora_server.domain.auth.service.AuthService;
import com.kit.memora_server.global.common.ApiResponse;
import com.kit.memora_server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse.UserInfo>> signup(@Valid @RequestBody SignupRequest request) {
        TokenResponse.UserInfo user = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("회원가입이 완료되었습니다.", user));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse token = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("로그인에 성공했습니다.", token));
    }

    @Operation(summary = "토큰 갱��")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse token = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(token));
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TokenResponse.UserInfo>> getMe(@AuthenticationPrincipal CustomUserDetails user) {
        TokenResponse.UserInfo me = authService.getMe(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(me));
    }
}
