package io.neulbo.backend.sleep.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SleepStage {
    AWAKE("깨어있음", 0),
    LIGHT_SLEEP("얕은잠", 1),
    DEEP_SLEEP("깊은잠", 2),
    REM("REM 수면", 3);

    private final String description;
    private final int depth;

    // 깊은 수면 단계인지 확인
    public boolean isDeepStage() {
        return this == DEEP_SLEEP;
    }

    // REM 수면인지 확인
    public boolean isREMStage() {
        return this == REM;
    }

    // 실제 수면 단계인지 확인 (깨어있지 않은 상태)
    public boolean isSleepStage() {
        return this != AWAKE;
    }
} 