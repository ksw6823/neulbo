package io.neulbo.backend.auth.dto;

// JWT 토큰을 포함한 로그인 응답 DTO
public record LoginResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {}