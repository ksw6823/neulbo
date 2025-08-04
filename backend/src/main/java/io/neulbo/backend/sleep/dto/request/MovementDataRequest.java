package io.neulbo.backend.sleep.dto.request;

import io.neulbo.backend.sleep.validation.ValidMovementTimestamp;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class MovementDataRequest {

    @NotNull(message = "움직임 데이터 목록은 필수입니다")
    @Size(min = 1, max = 1000, message = "움직임 데이터는 1개 이상 1000개 이하여야 합니다")
    @Valid
    private List<MovementEntry> movementData;

    public MovementDataRequest(List<MovementEntry> movementData) {
        this.movementData = movementData;
    }

    @Getter
    @NoArgsConstructor
    public static class MovementEntry {

        @NotNull(message = "타임스탬프는 필수입니다")
        @ValidMovementTimestamp(maxPastHours = 72, message = "움직임 데이터의 타임스탬프가 유효하지 않습니다")
        private LocalDateTime timestamp;

        @NotNull(message = "X축 가속도는 필수입니다")
        private Double accelerationX;

        @NotNull(message = "Y축 가속도는 필수입니다")
        private Double accelerationY;

        @NotNull(message = "Z축 가속도는 필수입니다")
        private Double accelerationZ;

        public MovementEntry(LocalDateTime timestamp, Double accelerationX, Double accelerationY, Double accelerationZ) {
            this.timestamp = timestamp;
            this.accelerationX = accelerationX;
            this.accelerationY = accelerationY;
            this.accelerationZ = accelerationZ;
        }
    }
} 