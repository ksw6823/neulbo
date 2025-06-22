package io.neulbo.backend.auth.dto;

// OAuth에서 사용자 정보를 담는 DTO
public record OAuthUser(
        String id,
        String nickname,
        String email,
        String profileImage
) {}