package io.neulbo.backend.sleep.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SessionStatus {
    IN_PROGRESS("진행 중"),
    COMPLETED("완료"),
    INTERRUPTED("중단됨"),
    CANCELLED("취소됨");

    private final String description;
} 