package io.neulbo.backend.sleep.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MovementType {
    STILL("정지", 0),
    LIGHT_MOVEMENT("가벼운 움직임", 1),
    MODERATE_MOVEMENT("보통 움직임", 2),
    STRONG_MOVEMENT("강한 움직임", 3);

    private final String description;
    private final int level;
} 