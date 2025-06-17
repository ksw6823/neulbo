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

import java.time.Duration;
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
        // 입력 파라미터 검증
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED);
        }

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
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED))
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            // 응답 결과 null 체크
            if (result == null) {
                throw new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED);
            }

            // 에러 응답 체크 (네이버는 에러 시 error 필드를 포함)
            if (result.containsKey("error")) {
                throw new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED);
            }

            // 필수 필드 access_token 검증
            Object accessTokenObj = result.get("access_token");
            if (accessTokenObj == null || !(accessTokenObj instanceof String)) {
                throw new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED);
            }
            
            String accessToken = (String) accessTokenObj;
            if (accessToken.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED);
            }

            // token_type 안전하게 추출
            String tokenType = null;
            Object tokenTypeObj = result.get("token_type");
            if (tokenTypeObj instanceof String) {
                tokenType = (String) tokenTypeObj;
            }

            // refresh_token 안전하게 추출
            String refreshToken = null;
            Object refreshTokenObj = result.get("refresh_token");
            if (refreshTokenObj instanceof String) {
                refreshToken = (String) refreshTokenObj;
            }

            // expires_in 안전하게 추출 및 검증
            Long expiresIn = 0L;
            Object expiresInObj = result.get("expires_in");
            if (expiresInObj instanceof Number) {
                long value = ((Number) expiresInObj).longValue();
                // 음수 값 검증
                if (value >= 0) {
                    expiresIn = value;
                }
            }

            return new OAuthToken(
                    accessToken,
                    tokenType,
                    refreshToken,
                    expiresIn
            );
        } catch (BusinessException e) {
            // BusinessException은 그대로 재던지기
            throw e;
        } catch (Exception e) {
            // 네트워크 오류, 타임아웃, JSON 파싱 오류 등 모든 예외 처리
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED);
        }
    }

    @Override
    public OAuthUser getUserInfo(String accessToken) {
        // 입력 파라미터 검증
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
        }

        try {
            Map<String, Object> response = webClient.get()
                    .uri(userInfoUri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            responseEntity -> responseEntity.bodyToMono(String.class)
                                    .map(body -> new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED))
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }

            // 에러 응답 체크 (네이버는 에러 시 error 필드를 포함)
            if (response.containsKey("error")) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }

            // 네이버 API response 구조: { "response": { "id": "...", ... } }
            Object responseObj = response.get("response");
            if (responseObj == null || !(responseObj instanceof Map)) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) responseObj;

            // 필수 필드 id 검증
            Object idObj = result.get("id");
            if (idObj == null || !(idObj instanceof String)) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }
            
            String socialId = (String) idObj;
            if (socialId.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }

            // nickname 안전하게 추출
            String nickname = "Unknown";
            Object nicknameObj = result.get("nickname");
            if (nicknameObj instanceof String && !((String) nicknameObj).trim().isEmpty()) {
                nickname = (String) nicknameObj;
            }

            // email 안전하게 추출
            String email = null;
            Object emailObj = result.get("email");
            if (emailObj instanceof String && !((String) emailObj).trim().isEmpty()) {
                email = (String) emailObj;
            }

            // profile_image 안전하게 추출
            String profileImage = null;
            Object profileImageObj = result.get("profile_image");
            if (profileImageObj instanceof String && !((String) profileImageObj).trim().isEmpty()) {
                profileImage = (String) profileImageObj;
            }

            return new OAuthUser(
                    socialId,
                    nickname,
                    email,
                    profileImage
            );
        } catch (BusinessException e) {
            // BusinessException은 그대로 재던지기
            throw e;
        } catch (Exception e) {
            // 네트워크 오류, 타임아웃, JSON 파싱 오류 등 모든 예외 처리
            throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
        }
    }


}
