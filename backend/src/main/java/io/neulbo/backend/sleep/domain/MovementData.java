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
@Table(name = "movement_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MovementData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sleep_session_id", nullable = false)
    @Setter(AccessLevel.PROTECTED)
    private SleepSession sleepSession;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "acceleration_x")
    private Double accelerationX;

    @Column(name = "acceleration_y")
    private Double accelerationY;

    @Column(name = "acceleration_z")
    private Double accelerationZ;

    @Column(name = "movement_intensity")
    private Double movementIntensity;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type")
    private MovementType movementType;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 움직임 데이터를 생성하는 정적 팩토리 메서드
     * 
     * @param timestamp 측정 시간 (필수)
     * @param x X축 가속도 (null인 경우 0.0으로 처리)
     * @param y Y축 가속도 (null인 경우 0.0으로 처리)
     * @param z Z축 가속도 (null인 경우 0.0으로 처리)
     * @return 생성된 MovementData 객체
     * @throws IllegalArgumentException timestamp가 null인 경우
     */
    public static MovementData create(LocalDateTime timestamp, Double x, Double y, Double z) {
        // timestamp는 필수값이므로 null 체크
        if (timestamp == null) {
            throw new IllegalArgumentException("측정 시간(timestamp)은 필수입니다");
        }
        
        // 좌표 값들의 null 체크 및 기본값 처리
        double safeX = x != null ? x : 0.0;
        double safeY = y != null ? y : 0.0;
        double safeZ = z != null ? z : 0.0;
        
        MovementData data = new MovementData();
        data.timestamp = timestamp;
        data.accelerationX = safeX;
        data.accelerationY = safeY;
        data.accelerationZ = safeZ;
        
        // 움직임 강도 계산 (가속도 벡터의 크기)
        data.movementIntensity = Math.sqrt(safeX * safeX + safeY * safeY + safeZ * safeZ);
        
        // 움직임 타입 분류
        data.movementType = classifyMovementType(data.movementIntensity);
        
        return data;
    }

    /**
     * 엄격한 검증을 적용하는 정적 팩토리 메서드
     * 모든 좌표 값이 null이 아니어야 합니다.
     * 
     * @param timestamp 측정 시간 (필수)
     * @param x X축 가속도 (필수)
     * @param y Y축 가속도 (필수)
     * @param z Z축 가속도 (필수)
     * @return 생성된 MovementData 객체
     * @throws IllegalArgumentException 어떤 매개변수라도 null인 경우
     */
    public static MovementData createStrict(LocalDateTime timestamp, Double x, Double y, Double z) {
        if (timestamp == null) {
            throw new IllegalArgumentException("측정 시간(timestamp)은 필수입니다");
        }
        if (x == null) {
            throw new IllegalArgumentException("X축 가속도는 필수입니다");
        }
        if (y == null) {
            throw new IllegalArgumentException("Y축 가속도는 필수입니다");
        }
        if (z == null) {
            throw new IllegalArgumentException("Z축 가속도는 필수입니다");
        }
        
        return create(timestamp, x, y, z);
    }

    private static MovementType classifyMovementType(Double intensity) {
        if (intensity < 0.5) {
            return MovementType.STILL;
        } else if (intensity < 1.5) {
            return MovementType.LIGHT_MOVEMENT;
        } else if (intensity < 3.0) {
            return MovementType.MODERATE_MOVEMENT;
        } else {
            return MovementType.STRONG_MOVEMENT;
        }
    }

    // 움직임이 있었는지 확인
    public boolean hasMovement() {
        return movementType != MovementType.STILL;
    }

    // 강한 움직임인지 확인
    public boolean isStrongMovement() {
        return movementType == MovementType.STRONG_MOVEMENT || 
               movementType == MovementType.MODERATE_MOVEMENT;
    }
} 