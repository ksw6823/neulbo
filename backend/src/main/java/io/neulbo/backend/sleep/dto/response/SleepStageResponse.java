package io.neulbo.backend.sleep.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.neulbo.backend.sleep.domain.SleepStage;
import io.neulbo.backend.sleep.domain.SleepStageData;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SleepStageResponse {

    private final Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime stageStartTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime stageEndTime;
    
    private final SleepStage sleepStage;
    private final Integer durationMinutes;
    private final Double confidenceScore;
    private final Integer movementCountDuringStage;

    public SleepStageResponse(SleepStageData stageData) {
        this.id = stageData.getId();
        this.stageStartTime = stageData.getStageStartTime();
        this.stageEndTime = stageData.getStageEndTime();
        this.sleepStage = stageData.getSleepStage();
        this.durationMinutes = stageData.getDurationMinutes();
        this.confidenceScore = stageData.getConfidenceScore();
        this.movementCountDuringStage = stageData.getMovementCountDuringStage();
    }
} 