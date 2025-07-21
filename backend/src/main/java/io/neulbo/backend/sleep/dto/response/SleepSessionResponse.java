package io.neulbo.backend.sleep.dto.response;

import io.neulbo.backend.sleep.domain.SleepQuality;
import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SessionStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class SleepSessionResponse {

    private final Long id;
    private final LocalDateTime sleepStartTime;
    private final LocalDateTime sleepEndTime;
    private final Integer intendedSleepDurationMinutes;
    private final Integer actualSleepDurationMinutes;
    private final Double sleepEfficiencyPercentage;
    private final Integer totalMovementCount;
    private final Integer wakeUpCount;
    private final SleepQuality sleepQuality;
    private final SessionStatus sessionStatus;
    private final String notes;
    private final LocalDateTime createdAt;
    private final List<SleepStageResponse> sleepStages;

    public SleepSessionResponse(SleepSession session) {
        this.id = session.getId();
        this.sleepStartTime = session.getSleepStartTime();
        this.sleepEndTime = session.getSleepEndTime();
        this.intendedSleepDurationMinutes = session.getIntendedSleepDurationMinutes();
        this.actualSleepDurationMinutes = session.getActualSleepDurationMinutes();
        this.sleepEfficiencyPercentage = session.getSleepEfficiencyPercentage();
        this.totalMovementCount = session.getTotalMovementCount();
        this.wakeUpCount = session.getWakeUpCount();
        this.sleepQuality = session.getSleepQuality();
        this.sessionStatus = session.getSessionStatus();
        this.notes = session.getNotes();
        this.createdAt = session.getCreatedAt();
        this.sleepStages = session.getSleepStages().stream()
                .map(SleepStageResponse::new)
                .collect(Collectors.toList());
    }

    // 간단한 정보만 포함하는 생성자 (목록 조회용)
    public static SleepSessionResponse createSimple(SleepSession session) {
        return new SleepSessionResponse(session.getId(),
                session.getSleepStartTime(),
                session.getSleepEndTime(),
                session.getIntendedSleepDurationMinutes(),
                session.getActualSleepDurationMinutes(),
                session.getSleepEfficiencyPercentage(),
                session.getTotalMovementCount(),
                session.getWakeUpCount(),
                session.getSleepQuality(),
                session.getSessionStatus(),
                session.getNotes(),
                session.getCreatedAt());
    }

    // 상세 정보 제외 생성자
    private SleepSessionResponse(Long id, LocalDateTime sleepStartTime, LocalDateTime sleepEndTime,
                                Integer intendedSleepDurationMinutes, Integer actualSleepDurationMinutes,
                                Double sleepEfficiencyPercentage, Integer totalMovementCount,
                                Integer wakeUpCount, SleepQuality sleepQuality, SessionStatus sessionStatus,
                                String notes, LocalDateTime createdAt) {
        this.id = id;
        this.sleepStartTime = sleepStartTime;
        this.sleepEndTime = sleepEndTime;
        this.intendedSleepDurationMinutes = intendedSleepDurationMinutes;
        this.actualSleepDurationMinutes = actualSleepDurationMinutes;
        this.sleepEfficiencyPercentage = sleepEfficiencyPercentage;
        this.totalMovementCount = totalMovementCount;
        this.wakeUpCount = wakeUpCount;
        this.sleepQuality = sleepQuality;
        this.sessionStatus = sessionStatus;
        this.notes = notes;
        this.createdAt = createdAt;
        this.sleepStages = null; // 상세 정보 제외
    }
} 