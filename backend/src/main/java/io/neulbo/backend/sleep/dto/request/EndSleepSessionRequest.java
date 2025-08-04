package io.neulbo.backend.sleep.dto.request;

import io.neulbo.backend.sleep.domain.SleepQuality;
import io.neulbo.backend.sleep.validation.ValidSleepEndTime;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class EndSleepSessionRequest {

    @NotNull(message = "수면 종료 시간은 필수입니다")
    @ValidSleepEndTime(maxPastDays = 7, message = "수면 종료 시간이 유효하지 않습니다")
    private LocalDateTime sleepEndTime;

    private SleepQuality sleepQuality;

    private String notes;

    public EndSleepSessionRequest(LocalDateTime sleepEndTime, SleepQuality sleepQuality, String notes) {
        this.sleepEndTime = sleepEndTime;
        this.sleepQuality = sleepQuality;
        this.notes = notes;
    }
} 