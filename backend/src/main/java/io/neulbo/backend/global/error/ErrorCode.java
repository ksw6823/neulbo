package io.neulbo.backend.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "E001", "잘못된 입력값입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "E002", "잘못된 타입입니다."),
    MISSING_SERVLET_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "E003", "필수 요청 파라미터가 누락되었습니다."),
    
    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E101", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "E102", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "E103", "만료된 토큰입니다."),
    
    // 403 Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "E201", "접근이 거부되었습니다."),
    
    // 404 Not Found
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "E301", "요청한 리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E302", "사용자를 찾을 수 없습니다."),
    
    // 409 Conflict
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "E401", "이미 존재하는 리소스입니다."),
    
    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E501", "서버 내부 오류가 발생했습니다."),
    
    // OAuth 관련
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "E601", "지원하지 않는 OAuth 제공자입니다."),
    OAUTH_TOKEN_REQUEST_FAILED(HttpStatus.BAD_REQUEST, "E602", "OAuth 토큰 요청에 실패했습니다."),
    OAUTH_USER_INFO_REQUEST_FAILED(HttpStatus.BAD_REQUEST, "E603", "OAuth 사용자 정보 요청에 실패했습니다.");
    
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
} 