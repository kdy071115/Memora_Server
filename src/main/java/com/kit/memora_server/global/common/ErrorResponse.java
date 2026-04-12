package com.kit.memora_server.global.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private boolean success;
    private int status;
    private String code;
    private String message;
    private LocalDateTime timestamp;

    public static ErrorResponse of(int status, String code, String message) {
        return ErrorResponse.builder()
                .success(false)
                .status(status)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
