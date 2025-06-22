package io.neulbo.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "blacklist:";

    public void blacklist(String token, long expireInMillis) {
        redisTemplate.opsForValue().set(PREFIX + token, "logout", Duration.ofMillis(expireInMillis));
    }

    public boolean isBlacklisted(String token) {
        return redisTemplate.hasKey(PREFIX + token);
    }
}
