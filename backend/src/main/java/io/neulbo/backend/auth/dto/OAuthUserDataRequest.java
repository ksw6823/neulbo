package io.neulbo.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * OAuth 사용자 데이터 직접 요청 DTO
 * 프론트엔드에서 OAuth 제공자로부터 직접 받아온 사용자 정보를 백엔드로 전달하기 위한 데이터 전송 객체
 */
public record OAuthUserDataRequest(
        @NotBlank(message = "OAuth 제공자는 필수입니다")
        @Size(min = 1, max = 20, message = "OAuth 제공자는 1-20자 사이여야 합니다")
        String provider,
        
        @NotBlank(message = "제공자 ID는 필수입니다")
        @Size(min = 1, max = 100, message = "제공자 ID는 1-100자 사이여야 합니다")
        String providerId,
        
        @Email(message = "올바른 이메일 형식이어야 합니다")
        @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
        String email,
        
        @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다")
        String name,
        
        @Size(max = 500, message = "프로필 이미지 URL은 500자를 초과할 수 없습니다")
        String profileImageUrl,
        
        @Size(max = 50, message = "닉네임은 50자를 초과할 수 없습니다")
        String nickname
) {
    /**
     * 필수 정보가 있는지 확인하는 헬퍼 메서드
     */
    public boolean hasRequiredFields() {
        return provider != null && !provider.trim().isEmpty() &&
               providerId != null && !providerId.trim().isEmpty();
    }
    
    /**
     * 지원되는 OAuth 제공자인지 확인하는 헬퍼 메서드
     */
    public boolean isSupportedProvider() {
        if (provider == null || provider.trim().isEmpty()) {
            return false;
        }
        
        String normalizedProvider = provider.toLowerCase().trim();
        return normalizedProvider.equals("google") || 
               normalizedProvider.equals("kakao") || 
               normalizedProvider.equals("naver");
    }
    
    /**
     * 정규화된 provider 이름 반환
     */
    public String getNormalizedProvider() {
        return provider != null ? provider.toLowerCase().trim() : null;
    }
    
    /**
     * 이메일이 있는지 확인하는 헬퍼 메서드
     */
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }
    
    /**
     * 이름이 있는지 확인하는 헬퍼 메서드
     */
    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }
    
    /**
     * 표시할 이름 결정 (이름 우선, 없으면 닉네임, 없으면 이메일 앞부분)
     */
    public String getDisplayName() {
        if (hasName()) {
            return name.trim();
        }
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname.trim();
        }
        if (hasEmail()) {
            return email.split("@")[0];
        }
        return "사용자";
    }
}