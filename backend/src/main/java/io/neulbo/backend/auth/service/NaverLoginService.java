package io.neulbo.backend.auth.service;

import io.neulbo.backend.auth.dto.OAuthToken;
import io.neulbo.backend.auth.dto.OAuthUser;
import io.neulbo.backend.auth.jwt.JwtProvider;
import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.exception.BusinessException;
import io.neulbo.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service("naver")
public class NaverLoginService extends AbstractOAuthLoginService {

    public NaverLoginService(WebClient webClient, UserRepository userRepository, JwtProvider jwtProvider) {
        super(webClient, userRepository, jwtProvider);
    }

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
        try {
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

            if (result == null) {
                throw new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED);
            }

            String accessToken = (String) result.get("access_token");
            if (accessToken == null) {
                throw new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED);
            }

            // expires_in null 체크 및 안전한 변환
            Long expiresIn = 0L;
            Object expiresInObj = result.get("expires_in");
            if (expiresInObj instanceof Number) {
                expiresIn = ((Number) expiresInObj).longValue();
            }

            return new OAuthToken(
                    accessToken,
                    (String) result.get("token_type"),
                    (String) result.get("refresh_token"),
                    expiresIn
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED);
        }
    }

    @Override
    public OAuthUser getUserInfo(String accessToken) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(userInfoUri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }

            Object responseObj = response.get("response");
            if (!(responseObj instanceof Map)) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) responseObj;

            String socialId = (String) result.get("id");
            if (socialId == null) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }

            return new OAuthUser(
                    socialId,
                    (String) result.getOrDefault("nickname", "Unknown"),
                    (String) result.get("email"),
                    (String) result.get("profile_image")
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
        }
    }


}
