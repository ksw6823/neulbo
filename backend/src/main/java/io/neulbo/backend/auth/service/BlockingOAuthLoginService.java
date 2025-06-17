package io.neulbo.backend.auth.service;

import io.neulbo.backend.auth.dto.OAuthToken;
import io.neulbo.backend.auth.dto.OAuthUser;
import io.neulbo.backend.auth.dto.LoginResponse;

/**
 * 블로킹 OAuth 로그인 서비스 인터페이스
 * 
 * 이 인터페이스는 하위 호환성을 위한 블로킹 API를 제공합니다.
 * 리액티브 체인 외부에서 사용되어야 하며, 내부적으로 리액티브 메서드를 
 * 적절한 스케줄러에서 블로킹 호출로 변환합니다.
 * 
 * @deprecated 새로운 코드에서는 OAuthLoginService의 리액티브 메서드 사용을 권장합니다.
 */
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