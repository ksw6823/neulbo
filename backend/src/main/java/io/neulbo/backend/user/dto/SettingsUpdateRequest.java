package io.neulbo.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 사용자 설정 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingsUpdateRequest {
    
    @NotNull(message = "프라이빗 설정은 필수입니다")
    private Boolean isPrivate;
} 