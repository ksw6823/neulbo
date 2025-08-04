package io.neulbo.backend.sleep.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class MovementDataTest {

    @Test
    @DisplayName("정상적인 값으로 MovementData 생성 - 성공")
    void shouldCreateMovementData_WhenAllValuesAreValid() {
        // given
        LocalDateTime timestamp = LocalDateTime.now();
        Double x = 1.0;
        Double y = 2.0;
        Double z = 3.0;

        // when
        MovementData result = MovementData.create(timestamp, x, y, z);

        // then
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getAccelerationX()).isEqualTo(x);
        assertThat(result.getAccelerationY()).isEqualTo(y);
        assertThat(result.getAccelerationZ()).isEqualTo(z);
        assertThat(result.getMovementIntensity()).isEqualTo(Math.sqrt(14.0)); // sqrt(1+4+9)
    }

    @Test
    @DisplayName("timestamp가 null인 경우 - 예외 발생")
    void shouldThrowException_WhenTimestampIsNull() {
        // given
        LocalDateTime timestamp = null;
        Double x = 1.0;
        Double y = 2.0;
        Double z = 3.0;

        // when & then
        assertThatThrownBy(() -> MovementData.create(timestamp, x, y, z))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("측정 시간(timestamp)은 필수입니다");
    }

    @Test
    @DisplayName("x 좌표가 null인 경우 - 0.0으로 처리")
    void shouldUseDefaultValue_WhenXIsNull() {
        // given
        LocalDateTime timestamp = LocalDateTime.now();
        Double x = null;
        Double y = 2.0;
        Double z = 3.0;

        // when
        MovementData result = MovementData.create(timestamp, x, y, z);

        // then
        assertThat(result.getAccelerationX()).isEqualTo(0.0);
        assertThat(result.getAccelerationY()).isEqualTo(y);
        assertThat(result.getAccelerationZ()).isEqualTo(z);
        assertThat(result.getMovementIntensity()).isEqualTo(Math.sqrt(13.0)); // sqrt(0+4+9)
    }

    @Test
    @DisplayName("모든 좌표가 null인 경우 - 0.0으로 처리")
    void shouldUseDefaultValues_WhenAllCoordinatesAreNull() {
        // given
        LocalDateTime timestamp = LocalDateTime.now();
        Double x = null;
        Double y = null;
        Double z = null;

        // when
        MovementData result = MovementData.create(timestamp, x, y, z);

        // then
        assertThat(result.getAccelerationX()).isEqualTo(0.0);
        assertThat(result.getAccelerationY()).isEqualTo(0.0);
        assertThat(result.getAccelerationZ()).isEqualTo(0.0);
        assertThat(result.getMovementIntensity()).isEqualTo(0.0);
        assertThat(result.getMovementType()).isEqualTo(MovementType.STILL);
    }

    @Test
    @DisplayName("createStrict 메서드 - 정상적인 값으로 생성 성공")
    void shouldCreateMovementDataStrict_WhenAllValuesAreValid() {
        // given
        LocalDateTime timestamp = LocalDateTime.now();
        Double x = 1.0;
        Double y = 2.0;
        Double z = 3.0;

        // when
        MovementData result = MovementData.createStrict(timestamp, x, y, z);

        // then
        assertThat(result.getAccelerationX()).isEqualTo(x);
        assertThat(result.getAccelerationY()).isEqualTo(y);
        assertThat(result.getAccelerationZ()).isEqualTo(z);
    }

    @Test
    @DisplayName("createStrict 메서드 - x가 null인 경우 예외 발생")
    void shouldThrowException_WhenXIsNullInStrictMode() {
        // given
        LocalDateTime timestamp = LocalDateTime.now();
        Double x = null;
        Double y = 2.0;
        Double z = 3.0;

        // when & then
        assertThatThrownBy(() -> MovementData.createStrict(timestamp, x, y, z))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("X축 가속도는 필수입니다");
    }

    @Test
    @DisplayName("createStrict 메서드 - y가 null인 경우 예외 발생")
    void shouldThrowException_WhenYIsNullInStrictMode() {
        // given
        LocalDateTime timestamp = LocalDateTime.now();
        Double x = 1.0;
        Double y = null;
        Double z = 3.0;

        // when & then
        assertThatThrownBy(() -> MovementData.createStrict(timestamp, x, y, z))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Y축 가속도는 필수입니다");
    }

    @Test
    @DisplayName("createStrict 메서드 - z가 null인 경우 예외 발생")
    void shouldThrowException_WhenZIsNullInStrictMode() {
        // given
        LocalDateTime timestamp = LocalDateTime.now();
        Double x = 1.0;
        Double y = 2.0;
        Double z = null;

        // when & then
        assertThatThrownBy(() -> MovementData.createStrict(timestamp, x, y, z))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Z축 가속도는 필수입니다");
    }
}