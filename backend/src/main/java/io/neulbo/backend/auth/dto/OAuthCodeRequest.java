package io.neulbo.backend.auth.dto;

// Flutter로부터 인가코드를 받을 DTO
public record OAuthCodeRequest(String code) {}
