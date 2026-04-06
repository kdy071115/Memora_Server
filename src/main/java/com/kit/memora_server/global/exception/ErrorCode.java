package com.kit.memora_server.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),

    // Resource Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "강의를 찾을 수 없습니다."),
    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "LECTURE_NOT_FOUND", "차시를 찾을 수 없습니다."),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "QA 세션을 찾을 수 없습니다."),
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND", "문제를 찾을 수 없습니다."),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_NOT_FOUND", "공지사항을 찾을 수 없습니다."),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "FEEDBACK_NOT_FOUND", "피드백을 찾을 수 없습니다."),
    INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "INVALID_INVITE_CODE", "유효하지 않은 초대 코드입니다."),

    // Business
    NOT_ENROLLED(HttpStatus.FORBIDDEN, "NOT_ENROLLED", "수강 등록이 필요합니다."),
    ALREADY_ENROLLED(HttpStatus.CONFLICT, "ALREADY_ENROLLED", "이미 수강 등록되었습니다."),
    DOCUMENT_PROCESSING(HttpStatus.ACCEPTED, "DOCUMENT_PROCESSING", "문서가 아직 처리 중입니다."),
    QUIZ_HAS_ATTEMPTS(HttpStatus.CONFLICT, "QUIZ_HAS_ATTEMPTS", "이미 학생 풀이 기록이 있어 삭제할 수 없습니다."),

    // External
    AI_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVER_ERROR", "AI 서버 연결에 실패했습니다."),
    FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_UPLOAD_ERROR", "파일 업로드에 실패했습니다."),

    // General
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
