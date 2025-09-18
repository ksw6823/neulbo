package io.neulbo.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

