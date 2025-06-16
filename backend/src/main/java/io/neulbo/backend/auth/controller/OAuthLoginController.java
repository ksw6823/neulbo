package io.neulbo.backend.auth.controller;

import io.neulbo.backend.auth.dto.*;
import io.neulbo.backend.auth.jwt.JwtProvider;
import io.neulbo.backend.auth.service.OAuthLoginService;
import io.neulbo.backend.auth.service.RefreshTokenService;
import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.exception.BusinessException;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthLoginController {

    private final Map<String, OAuthLoginService> loginServices;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login/{provider}")
    public ResponseEntity<LoginResponse> login(
            @PathVariable String provider,
            @RequestBody OAuthCodeRequest request) {
        OAuthLoginService service = loginServices.get(provider.toLowerCase());
        if (service == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }

        try {
            OAuthToken token = service.getToken(request.code());
            OAuthUser userInfo = service.getUserInfo(token.accessToken());

            Optional<User> existingUser = userRepository.findBySocialIdAndProvider(userInfo.id(), provider);
            boolean isNewUser = existingUser.isEmpty();

            User user = existingUser.orElseGet(() -> userRepository.save(User.builder()
                    .socialId(userInfo.id())
                    .email(userInfo.email())
                    .nickname(userInfo.nickname())
                    .profileImage(userInfo.profileImage())
                    .provider(provider)
                    .build()));

            String accessToken = jwtProvider.createAccessToken(user.getId());
            String refreshToken = jwtProvider.createRefreshToken(user.getId());
            refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

            return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, isNewUser));

        } catch (Exception e) {
            // OAuth 토큰 요청 실패 또는 사용자 정보 요청 실패를 통합 처리
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED);
        }
    }
}
