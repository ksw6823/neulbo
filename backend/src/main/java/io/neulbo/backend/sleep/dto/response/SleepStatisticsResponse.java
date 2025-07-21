package io.neulbo.backend.sleep.dto.response;

import lombok.Getter;

import java.util.Map;

@Getter
public class SleepStatisticsResponse {

    private final Double averageSleepDurationMinutes;
    private final Double averageSleepEfficiency;
    private final Integer totalSleepSessions;
    private final Integer completedSessions;
    private final Map<String, Long> sleepStageDistribution; // 단계별 총 시간
    private final Double averageDeepSleepPercentage;
    private final Double averageREMSleepPercentage;
    private final Double averageLightSleepPercentage;
    private final Integer averageWakeUpCount;
    private final Double averageMovementCount;

    public SleepStatisticsResponse(Double averageSleepDurationMinutes,
                                   Double averageSleepEfficiency,
                                   Integer totalSleepSessions,
                                   Integer completedSessions,
                                   Map<String, Long> sleepStageDistribution,
                                   Double averageDeepSleepPercentage,
                                   Double averageREMSleepPercentage,
                                   Double averageLightSleepPercentage,
                                   Integer averageWakeUpCount,
                                   Double averageMovementCount) {
        this.averageSleepDurationMinutes = averageSleepDurationMinutes;
        this.averageSleepEfficiency = averageSleepEfficiency;
        this.totalSleepSessions = totalSleepSessions;
        this.completedSessions = completedSessions;
        this.sleepStageDistribution = sleepStageDistribution;
        this.averageDeepSleepPercentage = averageDeepSleepPercentage;
        this.averageREMSleepPercentage = averageREMSleepPercentage;
        this.averageLightSleepPercentage = averageLightSleepPercentage;
        this.averageWakeUpCount = averageWakeUpCount;
        this.averageMovementCount = averageMovementCount;
    }
} 