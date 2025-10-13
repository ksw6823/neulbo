package io.neulbo.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * OAuth 인증 코드 요청 DTO
 * Flutter 앱으로부터 OAuth 인가 코드를 받기 위한 데이터 전송 객체
 * 
 * @deprecated Authorization Code 방식은 더 이상 사용되지 않습니다. 
 *             {@link OAuthUserDataRequest}를 사용하세요.
 */
@Deprecated(since = "2025-10-12", forRemoval = true)
public record OAuthCodeRequest(
        @NotBlank(message = "인증 코드는 필수입니다")
        @Size(min = 1, max = 2000, message = "인증 코드 길이는 1-2000자 사이여야 합니다")
        String code
) {}
