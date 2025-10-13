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
    
    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "E251", "지원하지 않는 HTTP 메서드입니다."),
    
    // 409 Conflict
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "E401", "이미 존재하는 리소스입니다."),
    
    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E501", "서버 내부 오류가 발생했습니다."),
    
    // OAuth 관련
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "E601", "지원하지 않는 OAuth 제공자입니다."),
    OAUTH_TOKEN_REQUEST_FAILED(HttpStatus.BAD_REQUEST, "E602", "OAuth 토큰 요청에 실패했습니다."),
    OAUTH_USER_INFO_REQUEST_FAILED(HttpStatus.BAD_REQUEST, "E603", "OAuth 사용자 정보 요청에 실패했습니다."),
    INVALID_OAUTH_USER_DATA(HttpStatus.BAD_REQUEST, "E604", "유효하지 않은 OAuth 사용자 데이터입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "E605", "이미 존재하는 이메일입니다."),
    USER_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E606", "사용자 생성에 실패했습니다."),
    OAUTH_LOGIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E607", "OAuth 로그인에 실패했습니다."),
    
    // 수면 관련
    SLEEP_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "E701", "수면 세션을 찾을 수 없습니다."),
    SLEEP_SESSION_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "E702", "이미 진행 중인 수면 세션이 있습니다."),
    CANNOT_DELETE_ACTIVE_SLEEP_SESSION(HttpStatus.BAD_REQUEST, "E703", "진행 중인 수면 세션은 삭제할 수 없습니다."),
    INVALID_MOVEMENT_DATA(HttpStatus.BAD_REQUEST, "E704", "유효하지 않은 움직임 데이터입니다."),
    SLEEP_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E705", "수면 분석에 실패했습니다.");
    
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
} 