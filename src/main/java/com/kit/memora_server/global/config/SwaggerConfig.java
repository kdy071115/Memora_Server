package com.kit.memora_server.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("https://api.memora.example.com").description("Production (placeholder)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME_NAME, jwtSecurityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Memora API")
                .description("""
                        ## Memora - AI 학습 코파일럿 백엔드 API

                        강의 자료 기반 RAG 질의응답, 자동 퀴즈 생성/채점, 학습 분석을 제공하는 Spring Boot 백엔드.

                        ### 인증
                        대부분의 엔드포인트는 JWT 인증이 필요합니다.
                        1. `/api/auth/signup` 또는 `/api/auth/login` 으로 토큰 발급
                        2. 우측 상단 **Authorize** 버튼에 `accessToken` 입력 (Bearer 자동 부착)

                        ### 역할
                        - `STUDENT` — 수강, 학습, 퀴즈 풀이, 본인 분석
                        - `INSTRUCTOR` — 강의/차시/자료 관리, 퀴즈 검수, 학급 분석, 공지/피드백 작성

                        ### 도메인
                        - **Auth**: 회원가입 / 로그인 / 토큰 재발급
                        - **Course**: 강의 CRUD, 초대 코드 발급/수강
                        - **Lecture / Document**: 차시·자료 관리 + PDF 업로드 + 요약
                        - **QA**: AI 질의응답 세션
                        - **Quiz**: 퀴즈 자동생성 / 수동 CRUD / 풀이 / 채점
                        - **Analysis**: 개인 분석 + 교직자 강의 대시보드 / 수강생 드릴다운
                        - **Notice**: 강의 공지사항 (교직자 작성, 수강생 조회)
                        - **Feedback**: 교직자 → 학생 1:1 피드백 + 읽음 처리

                        ### 공통 응답
                        모든 응답은 `{ success, message, data }` 형태의 `ApiResponse` 로 래핑됩니다.
                        에러 시 `success=false` + `code` + `message` 가 반환됩니다.
                        """)
                .version("v1.1.0")
                .contact(new Contact()
                        .name("Memora Team")
                        .url("https://github.com/kdy071115/Memora_Server"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(JWT_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Access Token (Bearer)");
    }
}
