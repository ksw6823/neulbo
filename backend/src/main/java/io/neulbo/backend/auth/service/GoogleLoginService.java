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

@Service("google")
public class GoogleLoginService extends AbstractOAuthLoginService {

    public GoogleLoginService(WebClient webClient, UserRepository userRepository, JwtProvider jwtProvider) {
        super(webClient, userRepository, jwtProvider);
    }

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

            // 에러 응답 체크 (Google은 에러 시 error 필드를 포함)
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
            Map<String, Object> result = webClient.get()
                    .uri(userInfoUri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED))
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (result == null) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }

            // 에러 응답 체크 (Google은 에러 시 error 필드를 포함)
            if (result.containsKey("error")) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }

            // 필수 필드 sub (socialId) 검증
            Object subObj = result.get("sub");
            if (subObj == null || !(subObj instanceof String)) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }
            
            String socialId = (String) subObj;
            if (socialId.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED);
            }

            // name 필드 안전하게 추출
            String name = "Unknown";
            Object nameObj = result.get("name");
            if (nameObj instanceof String && !((String) nameObj).trim().isEmpty()) {
                name = (String) nameObj;
            }

            // email 필드 안전하게 추출
            String email = null;
            Object emailObj = result.get("email");
            if (emailObj instanceof String && !((String) emailObj).trim().isEmpty()) {
                email = (String) emailObj;
            }

            // picture 필드 안전하게 추출
            String picture = null;
            Object pictureObj = result.get("picture");
            if (pictureObj instanceof String && !((String) pictureObj).trim().isEmpty()) {
                picture = (String) pictureObj;
            }

            return new OAuthUser(
                    socialId,
                    name,
                    email,
                    picture
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
