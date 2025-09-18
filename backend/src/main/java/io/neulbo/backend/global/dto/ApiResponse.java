package io.neulbo.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 표준화된 API 응답 형식
 * 모든 API 응답에 일관된 구조를 제공합니다.
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    protected ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 성공 응답 (데이터 포함)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", data);
    }
    
    /**
     * 성공 응답 (메시지 포함)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
    
    /**
     * 성공 응답 (데이터 없음)
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }
    
    /**
     * 실패 응답
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
    
    /**
     * 실패 응답 (데이터 포함)
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
    
    /**
     * 에러 코드를 포함한 실패 응답
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        ErrorDetail errorDetail = new ErrorDetail(code, message);
        return new ApiResponse<>(false, message, (T) errorDetail);
    }
    
    /**
     * 필드 에러를 포함한 실패 응답 (Validation 에러용)
     */
    public static <T> ApiResponse<T> error(String code, String message, List<FieldError> fieldErrors) {
        ErrorDetail errorDetail = new ErrorDetail(code, message, fieldErrors);
        return new ApiResponse<>(false, message, (T) errorDetail);
    }
    
    /**
     * BindingResult를 포함한 실패 응답 (Validation 에러용)
     */
    public static <T> ApiResponse<T> error(String code, String message, BindingResult bindingResult) {
        List<FieldError> fieldErrors = FieldError.of(bindingResult);
        return error(code, message, fieldErrors);
    }
    
    /**
     * 타입 미스매치 에러 응답
     */
    public static <T> ApiResponse<T> error(String code, String message, String field, String value, String reason) {
        List<FieldError> fieldErrors = FieldError.of(field, value, reason);
        return error(code, message, fieldErrors);
    }
}

/**
 * 페이징 응답을 위한 메타데이터
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class PageMetadata {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    
    public PageMetadata(int page, int size, long totalElements, int totalPages, boolean first, boolean last) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }
    
    public static PageMetadata of(org.springframework.data.domain.Page<?> page) {
        return new PageMetadata(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}

/**
 * 페이징된 API 응답
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class PagedApiResponse<T> extends ApiResponse<java.util.List<T>> {
    private PageMetadata pageInfo;
    
    private PagedApiResponse(boolean success, String message, java.util.List<T> data, PageMetadata pageInfo) {
        super(success, message, data);
        this.pageInfo = pageInfo;
    }
    
    public static <T> PagedApiResponse<T> success(org.springframework.data.domain.Page<T> page) {
        return new PagedApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", page.getContent(), PageMetadata.of(page));
    }
    
    public static <T> PagedApiResponse<T> success(String message, org.springframework.data.domain.Page<T> page) {
        return new PagedApiResponse<>(true, message, page.getContent(), PageMetadata.of(page));
    }
}

/**
 * 에러 상세 정보
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ErrorDetail {
    private String code;
    private String message;
    private List<FieldError> fieldErrors;
    
    public ErrorDetail(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public ErrorDetail(String code, String message, List<FieldError> fieldErrors) {
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }
}

/**
 * 필드 에러 정보
 */
@Getter
@NoArgsConstructor
class FieldError {
    private String field;
    private String value;
    private String reason;
    
    public FieldError(String field, String value, String reason) {
        this.field = field;
        this.value = value;
        this.reason = reason;
    }
    
    public static List<FieldError> of(String field, String value, String reason) {
        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new FieldError(field, value, reason));
        return fieldErrors;
    }
    
    public static List<FieldError> of(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> new FieldError(
                        error.getField(),
                        error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());
    }
}

