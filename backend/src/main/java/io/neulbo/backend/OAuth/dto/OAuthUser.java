package io.neulbo.backend.OAuth.dto;

public record OAuthUser(
        String id,
        String email,
        String nickname,
        String profileImage
) {}