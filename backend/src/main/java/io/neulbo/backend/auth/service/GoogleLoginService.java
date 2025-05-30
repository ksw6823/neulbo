package io.neulbo.backend.auth.service;

import io.neulbo.backend.auth.dto.LoginResponse;
import io.neulbo.backend.auth.dto.OAuthToken;
import io.neulbo.backend.auth.dto.OAuthUser;
import io.neulbo.backend.auth.jwt.JwtProvider;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

@Service("google")
@RequiredArgsConstructor
public class GoogleLoginService implements OAuthLoginService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

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
                (String) result.get("sub"),     // social_id
                (String) result.get("name"),    // nickname
                (String) result.get("email"),   // email
                (String) result.get("picture")  // profile image
        );
    }

    @Override
    public LoginResponse login(String code, String provider) {
        OAuthToken token = getToken(code);
        OAuthUser userInfo = getUserInfo(token.accessToken());

        boolean isNewUser = false;
        User user;

        Optional<User> existingUser = userRepository.findBySocialIdAndProvider(userInfo.id(), provider.toLowerCase());

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = userRepository.save(User.builder()
                    .provider(provider.toLowerCase())
                    .socialId(userInfo.id())
                    .nickname(userInfo.nickname())
                    .email(userInfo.email())
                    .profileImage(userInfo.profileImage())
                    .build());
            isNewUser = true;
        }

        return new LoginResponse(
                jwtProvider.createAccessToken(user.getId()),
                jwtProvider.createRefreshToken(user.getId()),
                isNewUser
        );
    }
}
