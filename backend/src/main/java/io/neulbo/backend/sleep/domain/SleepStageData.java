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
    @Setter(AccessLevel.PROTECTED)
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

    /**
     * 수면 단계를 종료합니다.
     * 
     * @param endTime 수면 단계 종료 시간
     * @throws IllegalArgumentException endTime이 null이거나 stageStartTime보다 이전인 경우
     */
    public void endStage(LocalDateTime endTime) {
        // endTime null 체크
        if (endTime == null) {
            throw new IllegalArgumentException("수면 단계 종료 시간은 필수입니다");
        }
        
        // stageStartTime이 설정되어 있는지 확인
        if (stageStartTime == null) {
            throw new IllegalArgumentException("수면 단계 시작 시간이 설정되지 않았습니다");
        }
        
        // endTime이 stageStartTime 이후인지 검증
        if (endTime.isBefore(stageStartTime)) {
            throw new IllegalArgumentException(
                String.format("수면 단계 종료 시간(%s)은 시작 시간(%s) 이후여야 합니다", 
                    endTime, stageStartTime)
            );
        }
        
        this.stageEndTime = endTime;
        
        // 지속 시간 계산 (안전하게 검증된 값들로 계산)
        long minutes = java.time.Duration.between(stageStartTime, endTime).toMinutes();
        
        // 음수 방지를 위한 추가 안전장치 (이론적으로는 위 검증으로 불가능하지만 안전을 위해)
        this.durationMinutes = Math.max(0, (int) minutes);
    }

    /**
     * 종료 시간이 유효한지 검증합니다. (예외를 발생시키지 않음)
     * 
     * @param endTime 검증할 종료 시간
     * @return 유효한 경우 true, 그렇지 않으면 false
     */
    public boolean isValidEndTime(LocalDateTime endTime) {
        if (endTime == null) {
            return false;
        }
        if (stageStartTime == null) {
            return false;
        }
        return !endTime.isBefore(stageStartTime);
    }

    /**
     * 수면 단계를 안전하게 종료합니다. (예외를 발생시키지 않음)
     * 
     * @param endTime 수면 단계 종료 시간
     * @return 성공적으로 종료된 경우 true, 유효하지 않은 endTime인 경우 false
     */
    public boolean tryEndStage(LocalDateTime endTime) {
        if (!isValidEndTime(endTime)) {
            return false;
        }
        
        this.stageEndTime = endTime;
        long minutes = java.time.Duration.between(stageStartTime, endTime).toMinutes();
        this.durationMinutes = Math.max(0, (int) minutes);
        return true;
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

    // 연관관계 편의 메서드 - SleepSession 설정
    public void assignToSleepSession(SleepSession sleepSession) {
        this.sleepSession = sleepSession;
        if (sleepSession != null) {
            sleepSession.addSleepStageData(this);
        }
    }

    // 연관관계 편의 메서드 - SleepSession에서 제거
    public void removeFromSleepSession() {
        if (this.sleepSession != null) {
            this.sleepSession.removeSleepStageData(this);
            this.sleepSession = null;
        }
    }
} 