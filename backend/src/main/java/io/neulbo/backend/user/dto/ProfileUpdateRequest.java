package io.neulbo.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 사용자 프로필 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {
    
    @Size(max = 100, message = "사용자명은 100자 이하여야 합니다")
    private String username;
    
    private String profileImage;
    
    private LocalDate birth;
    
    private Boolean isPrivate;
} 