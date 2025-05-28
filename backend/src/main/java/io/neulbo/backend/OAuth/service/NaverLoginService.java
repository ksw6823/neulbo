package io.neulbo.backend.OAuth.service;

import io.neulbo.backend.OAuth.dto.OAuthToken;
import io.neulbo.backend.OAuth.dto.OAuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service("naver")
@RequiredArgsConstructor
public class NaverLoginService implements OAuthLoginService {

    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.provider.naver.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.naver.user-info-uri}")
    private String userInfoUri;

    @Override
    public OAuthToken getToken(String code) {
        Map<String, Object> result = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(tokenUri)
                        .queryParam("grant_type", "authorization_code")
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .queryParam("code", code)
                        .queryParam("redirect_uri", redirectUri)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return new OAuthToken(
                (String) result.get("access_token"),
                (String) result.get("token_type"),
                (String) result.get("refresh_token"),
                ((Number) result.get("expires_in")).longValue()
        );
    }

    @Override
    public OAuthUser getUserInfo(String accessToken) {
        Map<String, Object> result = webClient.get()
                .uri(userInfoUri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map<String, Object> response = (Map<String, Object>) result.get("response");

        return new OAuthUser(
                (String) response.get("id"),
                (String) response.get("email"),
                (String) response.get("name"),
                null // 프로필 이미지 X
        );
    }
}