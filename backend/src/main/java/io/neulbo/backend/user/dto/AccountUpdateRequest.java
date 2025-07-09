package io.neulbo.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 계정 정보 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountUpdateRequest {
    
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(max = 100, message = "사용자명은 100자 이하여야 합니다")
    private String username;
} 