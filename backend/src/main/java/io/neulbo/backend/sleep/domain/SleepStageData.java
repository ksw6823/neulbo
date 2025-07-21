package io.neulbo.backend.sleep.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "sleep_stage_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SleepStageData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sleep_session_id", nullable = false)
    @Setter
    private SleepSession sleepSession;

    @Column(name = "stage_start_time", nullable = false)
    private LocalDateTime stageStartTime;

    @Column(name = "stage_end_time")
    private LocalDateTime stageEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_stage", nullable = false)
    private SleepStage sleepStage;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "movement_count_during_stage")
    private Integer movementCountDuringStage;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 정적 팩토리 메서드
    public static SleepStageData create(SleepStage stage, LocalDateTime startTime, Double confidence) {
        SleepStageData data = new SleepStageData();
        data.sleepStage = stage;
        data.stageStartTime = startTime;
        data.confidenceScore = confidence;
        data.movementCountDuringStage = 0;
        return data;
    }

    // 수면 단계 종료
    public void endStage(LocalDateTime endTime) {
        this.stageEndTime = endTime;
        if (stageStartTime != null && endTime != null) {
            this.durationMinutes = (int) java.time.Duration.between(stageStartTime, endTime).toMinutes();
        }
    }

    // 움직임 카운트 증가
    public void incrementMovementCount() {
        this.movementCountDuringStage++;
    }

    // 단계가 진행 중인지 확인
    public boolean isInProgress() {
        return stageEndTime == null;
    }

    // 깊은 잠인지 확인
    public boolean isDeepSleep() {
        return sleepStage == SleepStage.DEEP_SLEEP;
    }

    // REM 수면인지 확인
    public boolean isREM() {
        return sleepStage == SleepStage.REM;
    }
} 