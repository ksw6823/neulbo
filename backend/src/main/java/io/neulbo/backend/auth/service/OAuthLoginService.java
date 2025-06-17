package io.neulbo.backend.auth.service;

import io.neulbo.backend.auth.dto.OAuthToken;
import io.neulbo.backend.auth.dto.OAuthUser;
import io.neulbo.backend.auth.dto.LoginResponse;
import reactor.core.publisher.Mono;

/**
 * 리액티브 OAuth 로그인 서비스 인터페이스
 * 
 * 이 인터페이스는 완전한 비블로킹 리액티브 API를 제공합니다.
 * 모든 메서드는 Mono를 반환하여 리액티브 스트림 내에서 
 * 블로킹 없이 체이닝할 수 있습니다.
 */
public interface OAuthLoginService extends BlockingOAuthLoginService {

    /**
     * 인가 코드로 access token 교환 (리액티브)
     * 
     * @param code OAuth 인가 코드
     * @return OAuth 토큰을 담은 Mono
     */
    Mono<OAuthToken> getTokenReactive(String code);

    /**
     * access token으로 사용자 정보 조회 (리액티브)
     * 
     * @param accessToken OAuth access token
     * @return OAuth 사용자 정보를 담은 Mono
     */
    Mono<OAuthUser> getUserInfoReactive(String accessToken);

    /**
     * 전체 로그인 프로세스 (리액티브)
     * 
     * @param code OAuth 인가 코드
     * @param provider OAuth 제공자 (google, kakao, naver)
     * @return 로그인 응답을 담은 Mono (JWT 토큰 포함)
     */
    Mono<LoginResponse> loginReactive(String code, String provider);
}
