package io.neulbo.backend.auth.controller;

import io.neulbo.backend.auth.dto.*;
import io.neulbo.backend.auth.jwt.JwtProvider;
import io.neulbo.backend.auth.service.OAuthLoginService;
import io.neulbo.backend.auth.service.RefreshTokenService;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
            @RequestBody OAuthCodeRequest request
    ) {
        OAuthLoginService service = loginServices.get(provider.toLowerCase());
        if (service == null) return ResponseEntity.badRequest().build();

        OAuthToken token = service.getToken(request.code());
        OAuthUser userInfo = service.getUserInfo(token.accessToken());

        User user = userRepository
                .findBySocialIdAndProvider(userInfo.id(), provider)
                .orElseGet(() -> userRepository.save(User.builder()
                        .socialId(userInfo.id())
                        .email(userInfo.email())
                        .nickname(userInfo.nickname())
                        .profileImage(userInfo.profileImage())
                        .provider(provider)
                        .build()));

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, user.getId() == null));
    }
}
