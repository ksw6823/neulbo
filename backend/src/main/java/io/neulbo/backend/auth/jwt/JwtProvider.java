package io.neulbo.backend.auth.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    // 30분짜리 Access Token
    private final long accessTokenValidity = 1000 * 60 * 30;

    // 7일짜리 Refresh Token
    private final long refreshTokenValidity = 1000L * 60 * 60 * 24 * 7;

    // 주어진 사용자 ID를 기반으로 30분 동안 유효한 액세스 토큰을 생성
    public String createAccessToken(Long userId) {
        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenValidity))
                .sign(Algorithm.HMAC256(secret));
    }

    // 주어진 사용자 ID를 기반으로 7일 동안 유효한 리프레시 토큰을 생성
    public String createRefreshToken(Long userId) {
        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .sign(Algorithm.HMAC256(secret));
    }

    // 토큰에서 userId(subject)를 추출
    public Long getUserIdFromToken(String token) {
        DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token);

        return Long.valueOf(decodedJWT.getSubject());
    }

    // 주어진 토큰을 디코딩하고 검증 (만료 시각 등 추출용)
    public DecodedJWT decode(String token) {
        return JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token);
    }
}
