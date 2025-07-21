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
    @Setter
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

    // 정적 팩토리 메서드
    public static MovementData create(LocalDateTime timestamp, Double x, Double y, Double z) {
        MovementData data = new MovementData();
        data.timestamp = timestamp;
        data.accelerationX = x;
        data.accelerationY = y;
        data.accelerationZ = z;
        
        // 움직임 강도 계산 (가속도 벡터의 크기)
        data.movementIntensity = Math.sqrt(x * x + y * y + z * z);
        
        // 움직임 타입 분류
        data.movementType = classifyMovementType(data.movementIntensity);
        
        return data;
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