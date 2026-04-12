package com.kit.memora_server.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kit.memora_server.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인증되지 않은 요청에 대해 401 Unauthorized + JSON 본문을 반환합니다.
 * Spring Security 기본 EntryPoint는 403을 던지기 때문에, 프론트엔드의
 * "401 → refresh 토큰으로 재발급" 로직이 동작하지 않는 문제를 막기 위함.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.debug("Unauthorized request to {}: {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Void> body = ApiResponse.error("인증이 필요합니다.");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
