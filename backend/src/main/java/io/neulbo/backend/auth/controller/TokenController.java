package io.neulbo.backend.auth.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.neulbo.backend.auth.jwt.JwtProvider;
import io.neulbo.backend.auth.service.RefreshTokenService;
import io.neulbo.backend.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class TokenController {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService blacklistService;

    // 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받는 엔드포인트
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("Authorization") String refreshHeader) {
        if (refreshHeader == null || !refreshHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("리프레시 토큰이 필요합니다.");
        }

        String refreshToken = refreshHeader.substring(7);
        Long userId;

        try {
            userId = jwtProvider.getUserIdFromToken(refreshToken);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("유효하지 않은 리프레시 토큰입니다.");
        }

        if (!refreshTokenService.isValidRefreshToken(userId, refreshToken)) {
            return ResponseEntity.status(401).body("만료되었거나 조작된 토큰입니다.");
        }

        String newAccessToken = jwtProvider.createAccessToken(userId);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    // 로그아웃 엔드포인트
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String accessHeader) {
        if (accessHeader == null || !accessHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Access Token이 필요합니다.");
        }

        String accessToken = accessHeader.substring(7);
        long expireMillis;

        try {
            DecodedJWT decoded = jwtProvider.decode(accessToken);
            Long userId = Long.valueOf(decoded.getSubject());

            refreshTokenService.deleteRefreshToken(userId);

            expireMillis = decoded.getExpiresAt().getTime() - System.currentTimeMillis();
            if (expireMillis > 0) {
                blacklistService.blacklist(accessToken, expireMillis);
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body("잘못된 토큰입니다.");
        }

        return ResponseEntity.ok("로그아웃 성공");
    }
}
