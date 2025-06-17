package io.neulbo.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// JWT 토큰을 포함한 로그인 응답 DTO
public record LoginResponse(
        String accessToken,
        String refreshToken,
        @JsonProperty("isNewUser") boolean isNewUser // isNewUser 필드 JSON 이름 명시하기
) {}