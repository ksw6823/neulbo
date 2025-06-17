package io.neulbo.backend.auth.service;

import io.neulbo.backend.auth.dto.OAuthToken;
import io.neulbo.backend.auth.dto.OAuthUser;
import io.neulbo.backend.auth.dto.LoginResponse;
import reactor.core.publisher.Mono;

public interface OAuthLoginService {

    // 인가 코드로 access token 교환
    OAuthToken getToken(String code);

    // access token으로 사용자 정보 조회
    OAuthUser getUserInfo(String accessToken);

    // 리액티브 버전 - 인가 코드로 access token 교환
    Mono<OAuthToken> getTokenReactive(String code);

    // 리액티브 버전 - access token으로 사용자 정보 조회
    Mono<OAuthUser> getUserInfoReactive(String accessToken);

    // 전체 로그인 프로세스 (토큰 발급까지 포함)
    LoginResponse login(String code, String provider);

    // 리액티브 버전 - 전체 로그인 프로세스
    Mono<LoginResponse> loginReactive(String code, String provider);
}
