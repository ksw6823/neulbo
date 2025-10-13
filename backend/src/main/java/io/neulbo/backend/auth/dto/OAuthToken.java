package io.neulbo.backend.auth.dto;

/**
 * OAuth 토큰 정보를 담는 DTO
 * 
 * @deprecated Authorization Code 방식은 더 이상 사용되지 않습니다. 
 *             {@link OAuthUserDataRequest}를 사용하세요.
 */
@Deprecated(since = "2025-10-12", forRemoval = true)
public record OAuthToken(
        String accessToken,
        String tokenType,
        String refreshToken,
        long expiresIn
) {}