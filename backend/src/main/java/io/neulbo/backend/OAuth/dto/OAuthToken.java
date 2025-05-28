package io.neulbo.backend.OAuth.dto;

public record OAuthToken(
        String accessToken,
        String tokenType,
        String refreshToken,
        long expiresIn
) {}