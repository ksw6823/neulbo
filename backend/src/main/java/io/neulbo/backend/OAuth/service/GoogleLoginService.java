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

@Service("google")
@RequiredArgsConstructor
public class GoogleLoginService implements OAuthLoginService {

    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUri;

    @Override
    public OAuthToken getToken(String code) {
        Map<String, Object> result = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("redirect_uri", redirectUri)
                        .with("code", code))
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

        return new OAuthUser(
                (String) result.get("sub"),
                (String) result.get("email"),
                (String) result.get("name"),
                (String) result.get("picture")
        );
    }
}