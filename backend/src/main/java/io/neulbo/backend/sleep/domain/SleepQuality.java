package io.neulbo.backend.sleep.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SleepQuality {
    EXCELLENT("매우 좋음", 5),
    GOOD("좋음", 4),
    FAIR("보통", 3),
    POOR("나쁨", 2),
    VERY_POOR("매우 나쁨", 1);

    private final String description;
    private final int score;

    public static SleepQuality fromScore(int score) {
        for (SleepQuality quality : values()) {
            if (quality.score == score) {
                return quality;
            }
        }
        throw new IllegalArgumentException("Invalid sleep quality score: " + score);
    }
} 