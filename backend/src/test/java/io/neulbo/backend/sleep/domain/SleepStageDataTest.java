package io.neulbo.backend.sleep.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class SleepStageDataTest {

    @Test
    @DisplayName("정상적인 종료 시간으로 단계 종료 - 성공")
    void shouldEndStage_WhenValidEndTime() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 23, 30);
        
        SleepStageData stageData = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);

        // when
        stageData.endStage(endTime);

        // then
        assertThat(stageData.getStageEndTime()).isEqualTo(endTime);
        assertThat(stageData.getDurationMinutes()).isEqualTo(90); // 1시간 30분
        assertThat(stageData.isInProgress()).isFalse();
    }

    @Test
    @DisplayName("null 종료 시간으로 단계 종료 - 예외 발생")
    void shouldThrowException_WhenEndTimeIsNull() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        SleepStageData stageData = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);

        // when & then
        assertThatThrownBy(() -> stageData.endStage(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수면 단계 종료 시간은 필수입니다");
    }

    @Test
    @DisplayName("시작 시간보다 이전 종료 시간으로 단계 종료 - 예외 발생")
    void shouldThrowException_WhenEndTimeIsBeforeStartTime() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 21, 30); // 시작 시간보다 30분 이전
        
        SleepStageData stageData = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);

        // when & then
        assertThatThrownBy(() -> stageData.endStage(endTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수면 단계 종료 시간")
                .hasMessageContaining("시작 시간")
                .hasMessageContaining("이후여야 합니다");
    }

    @Test
    @DisplayName("시작 시간과 동일한 종료 시간 - 성공 (0분 지속)")
    void shouldEndStage_WhenEndTimeEqualsStartTime() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 22, 0); // 동일한 시간
        
        SleepStageData stageData = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);

        // when
        stageData.endStage(endTime);

        // then
        assertThat(stageData.getStageEndTime()).isEqualTo(endTime);
        assertThat(stageData.getDurationMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("isValidEndTime - 유효한 종료 시간")
    void shouldReturnTrue_WhenEndTimeIsValid() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 23, 0);
        
        SleepStageData stageData = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);

        // when & then
        assertThat(stageData.isValidEndTime(endTime)).isTrue();
    }

    @Test
    @DisplayName("isValidEndTime - null 종료 시간")
    void shouldReturnFalse_WhenEndTimeIsNull() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        SleepStageData stageData = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);

        // when & then
        assertThat(stageData.isValidEndTime(null)).isFalse();
    }

    @Test
    @DisplayName("isValidEndTime - 시작 시간보다 이전 종료 시간")
    void shouldReturnFalse_WhenEndTimeIsBeforeStartTime() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 21, 30);
        
        SleepStageData stageData = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);

        // when & then
        assertThat(stageData.isValidEndTime(endTime)).isFalse();
    }

    @Test
    @DisplayName("tryEndStage - 유효한 종료 시간으로 성공")
    void shouldReturnTrue_WhenTryEndStageWithValidTime() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 23, 0);
        
        SleepStageData stageData = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);

        // when
        boolean result = stageData.tryEndStage(endTime);

        // then
        assertThat(result).isTrue();
        assertThat(stageData.getStageEndTime()).isEqualTo(endTime);
        assertThat(stageData.getDurationMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("tryEndStage - 무효한 종료 시간으로 실패")
    void shouldReturnFalse_WhenTryEndStageWithInvalidTime() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 21, 30); // 시작 시간보다 이전
        
        SleepStageData stageData = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);

        // when
        boolean result = stageData.tryEndStage(endTime);

        // then
        assertThat(result).isFalse();
        assertThat(stageData.getStageEndTime()).isNull(); // 변경되지 않음
        assertThat(stageData.getDurationMinutes()).isNull(); // 변경되지 않음
        assertThat(stageData.isInProgress()).isTrue(); // 여전히 진행 중
    }

    @Test
    @DisplayName("tryEndStage - null 종료 시간으로 실패")
    void shouldReturnFalse_WhenTryEndStageWithNullTime() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        SleepStageData stageData = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);

        // when
        boolean result = stageData.tryEndStage(null);

        // then
        assertThat(result).isFalse();
        assertThat(stageData.getStageEndTime()).isNull();
        assertThat(stageData.isInProgress()).isTrue();
    }

    @Test
    @DisplayName("긴 시간 지속된 수면 단계 처리")
    void shouldHandleLongDurationStage() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 2, 6, 0); // 8시간 후
        
        SleepStageData stageData = SleepStageData.create(SleepStage.DEEP_SLEEP, startTime, 0.9);

        // when
        stageData.endStage(endTime);

        // then
        assertThat(stageData.getDurationMinutes()).isEqualTo(480); // 8시간 = 480분
    }
}