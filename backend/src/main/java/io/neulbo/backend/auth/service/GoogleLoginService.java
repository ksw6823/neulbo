package io.neulbo.backend.auth.service;

import io.neulbo.backend.auth.dto.OAuthToken;
import io.neulbo.backend.auth.dto.OAuthUser;
import io.neulbo.backend.auth.jwt.JwtProvider;
import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.exception.BusinessException;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
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
        // 리액티브 메서드를 호출하고 블로킹으로 변환 (하위 호환성을 위해)
        return getTokenReactive(code)
                .subscribeOn(Schedulers.boundedElastic())
                .block();
    }

    @Override
    public OAuthUser getUserInfo(String accessToken) {
        // 리액티브 메서드를 호출하고 블로킹으로 변환 (하위 호환성을 위해)
        return getUserInfoReactive(accessToken)
                .subscribeOn(Schedulers.boundedElastic())
                .block();
    }

    @Override
    public Mono<OAuthToken> getTokenReactive(String code) {
        // 입력 파라미터 검증
        if (code == null || code.trim().isEmpty()) {
            return Mono.error(new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED));
        }

        return webClient.post()
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
                        response -> {
                            int statusCode = response.statusCode().value();
                            return response.bodyToMono(String.class)
                                    .doOnNext(body -> {
                                        // 응답 본문을 안전하게 자르기 (최대 500자)
                                        String truncatedBody = body != null && body.length() > 500 
                                                ? body.substring(0, 500) + "..." 
                                                : body;
                                        log.error("Google token request failed - Status: {}, Response: {}", 
                                                statusCode, truncatedBody);
                                    })
                                    .map(body -> new BusinessException(
                                            String.format("Google token request failed with status %d: %s", 
                                                    statusCode, 
                                                    body != null && body.length() > 100 
                                                            ? body.substring(0, 100) + "..." 
                                                            : body),
                                            ErrorCode.OAUTH_TOKEN_REQUEST_FAILED));
                        }
                )
                .bodyToMono(Map.class)
                .map(this::mapToOAuthToken)
                .onErrorMap(Exception.class, e -> {
                    if (e instanceof BusinessException) {
                        return e;
                    }
                    log.error("Google token request failed with exception", e);
                    return new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED, e);
                });
    }

    @Override
    public Mono<OAuthUser> getUserInfoReactive(String accessToken) {
        // 입력 파라미터 검증
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return Mono.error(new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED));
        }

        return webClient.get()
                .uri(userInfoUri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            int statusCode = response.statusCode().value();
                            return response.bodyToMono(String.class)
                                    .doOnNext(body -> {
                                        // 응답 본문을 안전하게 자르기 (최대 500자)
                                        String truncatedBody = body != null && body.length() > 500 
                                                ? body.substring(0, 500) + "..." 
                                                : body;
                                        log.error("Google user info request failed - Status: {}, Response: {}", 
                                                statusCode, truncatedBody);
                                    })
                                    .map(body -> new BusinessException(
                                            String.format("Google user info request failed with status %d: %s", 
                                                    statusCode, 
                                                    body != null && body.length() > 100 
                                                            ? body.substring(0, 100) + "..." 
                                                            : body),
                                            ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED));
                        }
                )
                .bodyToMono(Map.class)
                .map(this::mapToOAuthUser)
                .onErrorMap(Exception.class, e -> {
                    if (e instanceof BusinessException) {
                        return e;
                    }
                    log.error("Google user info request failed with exception", e);
                    return new BusinessException(ErrorCode.OAUTH_USER_INFO_REQUEST_FAILED, e);
                });
    }

    /**
     * Map을 OAuthToken으로 변환하는 헬퍼 메서드
     */
    private OAuthToken mapToOAuthToken(Map<String, Object> result) {
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
    }

    /**
     * Map을 OAuthUser로 변환하는 헬퍼 메서드
     */
    private OAuthUser mapToOAuthUser(Map<String, Object> result) {
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
    }

}
