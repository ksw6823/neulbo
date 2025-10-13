package io.neulbo.backend.auth.service;

import io.neulbo.backend.auth.dto.OAuthToken;
import io.neulbo.backend.auth.dto.OAuthUser;
import io.neulbo.backend.auth.dto.LoginResponse;

/**
 * 블로킹 방식 OAuth 로그인 서비스 인터페이스
 * 
 * 이 인터페이스는 전통적인 블로킹 API를 제공합니다.
 * 성능상의 이유로 리액티브 방식을 우선 사용하는 것을 권장하지만,
 * 기존 코드와의 호환성을 위해 제공됩니다.
 * 
 * @deprecated Authorization Code 방식은 더 이상 사용되지 않습니다. 
 *             {@link DirectOAuthLoginService}를 사용하세요.
 */
@Deprecated(since = "2025-10-12", forRemoval = true)
public interface BlockingOAuthLoginService {

    /**
     * 인가 코드로 access token 교환 (블로킹)
     * 
     * @param code OAuth 인가 코드
     * @return OAuth 토큰
     * @deprecated getTokenReactive() 사용을 권장합니다.
     */
    OAuthToken getToken(String code);

    /**
     * access token으로 사용자 정보 조회 (블로킹)
     * 
     * @param accessToken OAuth access token
     * @return OAuth 사용자 정보
     * @deprecated getUserInfoReactive() 사용을 권장합니다.
     */
    OAuthUser getUserInfo(String accessToken);

    /**
     * 전체 로그인 프로세스 (블로킹)
     * 
     * @param code OAuth 인가 코드
     * @param provider OAuth 제공자 (google, kakao, naver)
     * @return 로그인 응답 (JWT 토큰 포함)
     * @deprecated loginReactive() 사용을 권장합니다.
     */
    LoginResponse login(String code, String provider);
} 