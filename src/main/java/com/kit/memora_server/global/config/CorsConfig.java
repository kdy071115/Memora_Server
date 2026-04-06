package com.kit.memora_server.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * 허용할 origin 패턴 목록 (콤마로 구분).
     * 와일드카드 지원 - 예시:
     *   http://localhost:3000
     *   https://*.vercel.app
     *   https://memora-frontend-*.vercel.app
     *   https://*.trycloudflare.com
     */
    @Value("${cors.allowed-origin-patterns:http://localhost:3000,http://localhost:5173}")
    private String allowedOriginPatterns;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> patterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOriginPatterns(patterns);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
