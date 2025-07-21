package io.neulbo.backend.sleep.domain;

import io.neulbo.backend.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sleep_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SleepSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sleep_start_time", nullable = false)
    private LocalDateTime sleepStartTime;

    @Column(name = "sleep_end_time")
    private LocalDateTime sleepEndTime;

    @Column(name = "intended_sleep_duration_minutes")
    private Integer intendedSleepDurationMinutes;

    @Column(name = "actual_sleep_duration_minutes")
    private Integer actualSleepDurationMinutes;

    @Column(name = "sleep_efficiency_percentage")
    private Double sleepEfficiencyPercentage;

    @Column(name = "total_movement_count")
    private Integer totalMovementCount;

    @Column(name = "wake_up_count")
    private Integer wakeUpCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_quality")
    private SleepQuality sleepQuality;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status")
    private SessionStatus sessionStatus;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "sleepSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SleepStageData> sleepStages = new ArrayList<>();

    @OneToMany(mappedBy = "sleepSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovementData> movementData = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 정적 팩토리 메서드
    public static SleepSession createSession(User user, LocalDateTime sleepStartTime, Integer intendedDuration) {
        SleepSession session = new SleepSession();
        session.user = user;
        session.sleepStartTime = sleepStartTime;
        session.intendedSleepDurationMinutes = intendedDuration;
        session.sessionStatus = SessionStatus.IN_PROGRESS;
        session.totalMovementCount = 0;
        session.wakeUpCount = 0;
        return session;
    }

    // 수면 종료
    public void endSleep(LocalDateTime endTime) {
        this.sleepEndTime = endTime;
        this.sessionStatus = SessionStatus.COMPLETED;
        
        if (sleepStartTime != null && endTime != null) {
            this.actualSleepDurationMinutes = (int) java.time.Duration.between(sleepStartTime, endTime).toMinutes();
            
            // 수면 효율 계산 (실제 수면 시간 / 침대에 있었던 시간)
            if (intendedSleepDurationMinutes != null && intendedSleepDurationMinutes > 0) {
                this.sleepEfficiencyPercentage = (double) actualSleepDurationMinutes / intendedSleepDurationMinutes * 100;
            }
        }
    }

    // 움직임 데이터 추가
    public void addMovementData(MovementData movement) {
        this.movementData.add(movement);
        movement.setSleepSession(this);
        this.totalMovementCount++;
    }

    // 수면 단계 데이터 추가
    public void addSleepStageData(SleepStageData stageData) {
        this.sleepStages.add(stageData);
        stageData.setSleepSession(this);
    }

    // 수면 품질 업데이트
    public void updateSleepQuality(SleepQuality quality) {
        this.sleepQuality = quality;
    }

    // 노트 추가
    public void addNotes(String notes) {
        this.notes = notes;
    }

    // 깨어난 횟수 증가
    public void incrementWakeUpCount() {
        this.wakeUpCount++;
    }

    // 수면 중인지 확인
    public boolean isInProgress() {
        return sessionStatus == SessionStatus.IN_PROGRESS;
    }

    // 수면 완료 여부 확인
    public boolean isCompleted() {
        return sessionStatus == SessionStatus.COMPLETED;
    }
} 