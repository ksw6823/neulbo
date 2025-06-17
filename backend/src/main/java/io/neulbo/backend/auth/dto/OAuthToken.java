package io.neulbo.backend.auth.dto;

// OAuth 토큰 정보를 담는 DTO
public record OAuthToken(
        String accessToken,
        String tokenType,
        String refreshToken,
        long expiresIn
) {}