package io.neulbo.backend.global.exception;

import com.auth0.jwt.exceptions.JWTVerificationException;
import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * javax.validation.Valid or @Validated 으로 binding error 발생시 발생한다.
     * HttpMessageConverter 에서 등록한 HttpMessageConverter binding 못할경우 발생
     * 주로 @RequestBody, @RequestPart 어노테이션에서 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("handleMethodArgumentNotValidException", e);
        final ApiResponse<Object> response = ApiResponse.error(
            ErrorCode.INVALID_INPUT_VALUE.getCode(),
            ErrorCode.INVALID_INPUT_VALUE.getMessage(),
            e.getBindingResult()
        );
        return new ResponseEntity<>(response, ErrorCode.INVALID_INPUT_VALUE.getHttpStatus());
    }
    
    /**
     * enum type 일치하지 않아 binding 못할 경우 발생
     * 주로 @RequestParam enum으로 binding 못했을 경우 발생
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("handleMethodArgumentTypeMismatchException", e);
        final String value = e.getValue() == null ? "" : e.getValue().toString();
        final ApiResponse<Object> response = ApiResponse.error(
            ErrorCode.INVALID_TYPE_VALUE.getCode(),
            ErrorCode.INVALID_TYPE_VALUE.getMessage(),
            e.getName(), value, e.getErrorCode()
        );
        return new ResponseEntity<>(response, ErrorCode.INVALID_TYPE_VALUE.getHttpStatus());
    }
    
    /**
     * 지원하지 않은 HTTP method 호출 할 경우 발생
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("handleHttpRequestMethodNotSupportedException", e);
        final ApiResponse<Object> response = ApiResponse.error(
            ErrorCode.METHOD_NOT_ALLOWED.getCode(),
            ErrorCode.METHOD_NOT_ALLOWED.getMessage()
        );
        return new ResponseEntity<>(response, ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus());
    }
    
    /**
     * @RequestParam 으로 데이터가 넘어오지 않았을 경우
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("handleMissingServletRequestParameterException", e);
        final ApiResponse<Object> response = ApiResponse.error(
            ErrorCode.MISSING_SERVLET_REQUEST_PARAMETER.getCode(),
            ErrorCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage()
        );
        return new ResponseEntity<>(response, ErrorCode.MISSING_SERVLET_REQUEST_PARAMETER.getHttpStatus());
    }
    
    /**
     * JSON parse error
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("handleHttpMessageNotReadableException", e);
        final ApiResponse<Object> response = ApiResponse.error(
            ErrorCode.INVALID_INPUT_VALUE.getCode(),
            ErrorCode.INVALID_INPUT_VALUE.getMessage()
        );
        return new ResponseEntity<>(response, ErrorCode.INVALID_INPUT_VALUE.getHttpStatus());
    }
    
    /**
     * JWT 토큰 검증 실패
     */
    @ExceptionHandler(JWTVerificationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleJWTVerificationException(JWTVerificationException e) {
        log.error("handleJWTVerificationException", e);
        final ApiResponse<Object> response = ApiResponse.error(
            ErrorCode.INVALID_TOKEN.getCode(),
            ErrorCode.INVALID_TOKEN.getMessage()
        );
        return new ResponseEntity<>(response, ErrorCode.INVALID_TOKEN.getHttpStatus());
    }
    
    /**
     * 비즈니스 로직 실행 중 오류 발생
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Object>> handleBusinessException(final BusinessException e) {
        log.error("handleBusinessException", e);
        final ErrorCode errorCode = e.getErrorCode();
        final ApiResponse<Object> response = ApiResponse.error(
            errorCode.getCode(),
            errorCode.getMessage()
        );
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }
    
    /**
     * 나머지 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("handleException", e);
        final ApiResponse<Object> response = ApiResponse.error(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );
        return new ResponseEntity<>(response, ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus());
    }
} 