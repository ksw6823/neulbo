package io.neulbo.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "refresh:";
    private static final Duration TTL = Duration.ofDays(7);

    public void saveRefreshToken(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(PREFIX + userId, refreshToken, TTL);
    }

    public boolean isValidRefreshToken(Long userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(PREFIX + userId);
        return stored != null && stored.equals(refreshToken);
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(PREFIX + userId);
    }
}
