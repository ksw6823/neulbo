package io.neulbo.backend.sleep.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class StartSleepSessionRequest {

    @NotNull(message = "수면 시작 시간은 필수입니다")
    private LocalDateTime sleepStartTime;

    @Min(value = 30, message = "예상 수면 시간은 최소 30분 이상이어야 합니다")
    @Max(value = 720, message = "예상 수면 시간은 최대 12시간까지 가능합니다")
    private Integer intendedSleepDurationMinutes;

    private String notes;

    public StartSleepSessionRequest(LocalDateTime sleepStartTime, Integer intendedSleepDurationMinutes, String notes) {
        this.sleepStartTime = sleepStartTime;
        this.intendedSleepDurationMinutes = intendedSleepDurationMinutes;
        this.notes = notes;
    }
} 