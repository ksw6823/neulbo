package io.neulbo.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "refresh:";
    private static final Duration TTL = Duration.ofDays(7);

    public void saveRefreshToken(UUID userId, String refreshToken) {
        redisTemplate.opsForValue().set(PREFIX + userId.toString(), refreshToken, TTL);
    }

    public boolean isValidRefreshToken(UUID userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(PREFIX + userId.toString());
        return stored != null && stored.equals(refreshToken);
    }

    public void deleteRefreshToken(UUID userId) {
        redisTemplate.delete(PREFIX + userId.toString());
    }
}
