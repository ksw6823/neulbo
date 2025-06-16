package io.neulbo.backend.auth.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtProvider {

    // 메모리 효율성을 위해 static final로 선언
    private final String secret;

    // 30분짜리 Access Token
    private static final long ACCESS_TOKEN_VALIDITY = 1000 * 60 * 30;

    // 7일짜리 Refresh Token
    private static final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 7;

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    // 주어진 사용자 ID를 기반으로 30분 동안 유효한 액세스 토큰을 생성
    public String createAccessToken(Long userId) {
        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .sign(Algorithm.HMAC256(secret));
    }

    // 주어진 사용자 ID를 기반으로 7일 동안 유효한 리프레시 토큰을 생성
    public String createRefreshToken(Long userId) {
        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
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
