package io.neulbo.backend.auth.service;

import io.neulbo.backend.auth.dto.LoginResponse;
import io.neulbo.backend.auth.dto.OAuthToken;
import io.neulbo.backend.auth.dto.OAuthUser;
import io.neulbo.backend.auth.jwt.JwtProvider;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@RequiredArgsConstructor
public abstract class AbstractOAuthLoginService implements OAuthLoginService {

    protected final WebClient webClient;
    protected final UserRepository userRepository;
    protected final JwtProvider jwtProvider;

    /**
     * 공통 로그인 로직
     * 1. 토큰 획득
     * 2. 사용자 정보 조회
     * 3. 사용자 조회/생성
     * 4. JWT 토큰 생성 및 반환
     */
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

    // 각 구현체에서 제공자별 로직을 구현해야 하는 추상 메서드들
    @Override
    public abstract OAuthToken getToken(String code);

    @Override
    public abstract OAuthUser getUserInfo(String accessToken);
} 