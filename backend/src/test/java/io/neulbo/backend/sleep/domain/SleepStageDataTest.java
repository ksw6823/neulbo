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

    // ========================== incrementMovementCount() 테스트 ==========================

    @Test
    @DisplayName("incrementMovementCount - 움직임 카운트 정상 증가")
    void shouldIncrementMovementCount_WhenCalled() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        SleepStageData stageData = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);
        
        // 초기값 확인
        assertThat(stageData.getMovementCountDuringStage()).isEqualTo(0);

        // when
        stageData.incrementMovementCount();

        // then
        assertThat(stageData.getMovementCountDuringStage()).isEqualTo(1);
    }

    @Test
    @DisplayName("incrementMovementCount - 여러 번 호출 시 누적 증가")
    void shouldAccumulateMovementCount_WhenCalledMultipleTimes() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        SleepStageData stageData = SleepStageData.create(SleepStage.DEEP_SLEEP, startTime, 0.9);

        // when
        stageData.incrementMovementCount();
        stageData.incrementMovementCount();
        stageData.incrementMovementCount();

        // then
        assertThat(stageData.getMovementCountDuringStage()).isEqualTo(3);
    }

    @Test
    @DisplayName("incrementMovementCount - 대량 증가 처리")
    void shouldHandleLargeMovementCount_WhenIncrementedManyTimes() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        SleepStageData stageData = SleepStageData.create(SleepStage.REM, startTime, 0.7);
        int incrementCount = 100;

        // when
        for (int i = 0; i < incrementCount; i++) {
            stageData.incrementMovementCount();
        }

        // then
        assertThat(stageData.getMovementCountDuringStage()).isEqualTo(incrementCount);
    }

    // ========================== 수면 단계 상태 확인 메서드 테스트 ==========================

    @Test
    @DisplayName("isDeepSleep - DEEP_SLEEP 단계에서 true 반환")
    void shouldReturnTrue_WhenIsDeepSleepCalledOnDeepSleepStage() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        SleepStageData deepSleepStage = SleepStageData.create(SleepStage.DEEP_SLEEP, startTime, 0.9);

        // when & then
        assertThat(deepSleepStage.isDeepSleep()).isTrue();
    }

    @Test
    @DisplayName("isDeepSleep - 다른 수면 단계에서 false 반환")
    void shouldReturnFalse_WhenIsDeepSleepCalledOnNonDeepSleepStages() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        
        SleepStageData lightSleepStage = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);
        SleepStageData remStage = SleepStageData.create(SleepStage.REM, startTime, 0.7);
        SleepStageData awakeStage = SleepStageData.create(SleepStage.AWAKE, startTime, 0.6);

        // when & then
        assertThat(lightSleepStage.isDeepSleep()).isFalse();
        assertThat(remStage.isDeepSleep()).isFalse();
        assertThat(awakeStage.isDeepSleep()).isFalse();
    }

    @Test
    @DisplayName("isREM - REM 단계에서 true 반환")
    void shouldReturnTrue_WhenIsREMCalledOnREMStage() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        SleepStageData remStage = SleepStageData.create(SleepStage.REM, startTime, 0.7);

        // when & then
        assertThat(remStage.isREM()).isTrue();
    }

    @Test
    @DisplayName("isREM - 다른 수면 단계에서 false 반환")
    void shouldReturnFalse_WhenIsREMCalledOnNonREMStages() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        
        SleepStageData lightSleepStage = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);
        SleepStageData deepSleepStage = SleepStageData.create(SleepStage.DEEP_SLEEP, startTime, 0.9);
        SleepStageData awakeStage = SleepStageData.create(SleepStage.AWAKE, startTime, 0.6);

        // when & then
        assertThat(lightSleepStage.isREM()).isFalse();
        assertThat(deepSleepStage.isREM()).isFalse();
        assertThat(awakeStage.isREM()).isFalse();
    }

    @Test
    @DisplayName("수면 단계 상태 확인 - 모든 단계별 정확한 식별")
    void shouldCorrectlyIdentifyAllSleepStageTypes() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        
        SleepStageData awakeStage = SleepStageData.create(SleepStage.AWAKE, startTime, 0.6);
        SleepStageData lightSleepStage = SleepStageData.create(SleepStage.LIGHT_SLEEP, startTime, 0.8);
        SleepStageData deepSleepStage = SleepStageData.create(SleepStage.DEEP_SLEEP, startTime, 0.9);
        SleepStageData remStage = SleepStageData.create(SleepStage.REM, startTime, 0.7);

        // when & then - AWAKE 단계 검증
        assertThat(awakeStage.getSleepStage()).isEqualTo(SleepStage.AWAKE);
        assertThat(awakeStage.isDeepSleep()).isFalse();
        assertThat(awakeStage.isREM()).isFalse();

        // LIGHT_SLEEP 단계 검증
        assertThat(lightSleepStage.getSleepStage()).isEqualTo(SleepStage.LIGHT_SLEEP);
        assertThat(lightSleepStage.isDeepSleep()).isFalse();
        assertThat(lightSleepStage.isREM()).isFalse();

        // DEEP_SLEEP 단계 검증
        assertThat(deepSleepStage.getSleepStage()).isEqualTo(SleepStage.DEEP_SLEEP);
        assertThat(deepSleepStage.isDeepSleep()).isTrue();
        assertThat(deepSleepStage.isREM()).isFalse();

        // REM 단계 검증
        assertThat(remStage.getSleepStage()).isEqualTo(SleepStage.REM);
        assertThat(remStage.isDeepSleep()).isFalse();
        assertThat(remStage.isREM()).isTrue();
    }

    // ========================== SleepStageData.create() 팩토리 메서드 검증 테스트 ==========================

    @Test
    @DisplayName("create - null SleepStage로 생성 시 예외 발생")
    void shouldThrowException_WhenCreateWithNullSleepStage() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        Double confidence = 0.8;

        // when & then
        assertThatThrownBy(() -> SleepStageData.create(null, startTime, confidence))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("create - null 시작 시간으로 생성 시 정상 처리 (지연 설정 가능)")
    void shouldAllowCreation_WhenCreateWithNullStartTime() {
        // given
        SleepStage stage = SleepStage.LIGHT_SLEEP;
        Double confidence = 0.8;

        // when
        SleepStageData stageData = SleepStageData.create(stage, null, confidence);

        // then
        assertThat(stageData).isNotNull();
        assertThat(stageData.getSleepStage()).isEqualTo(stage);
        assertThat(stageData.getStageStartTime()).isNull();
        assertThat(stageData.getConfidenceScore()).isEqualTo(confidence);
        assertThat(stageData.getMovementCountDuringStage()).isEqualTo(0);
    }

    @Test
    @DisplayName("create - null confidence로 생성 시 정상 처리")
    void shouldAllowCreation_WhenCreateWithNullConfidence() {
        // given
        SleepStage stage = SleepStage.DEEP_SLEEP;
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);

        // when
        SleepStageData stageData = SleepStageData.create(stage, startTime, null);

        // then
        assertThat(stageData).isNotNull();
        assertThat(stageData.getSleepStage()).isEqualTo(stage);
        assertThat(stageData.getStageStartTime()).isEqualTo(startTime);
        assertThat(stageData.getConfidenceScore()).isNull();
        assertThat(stageData.getMovementCountDuringStage()).isEqualTo(0);
    }

    @Test
    @DisplayName("create - 음수 confidence 값으로 생성 시 정상 처리 (검증은 별도 로직에서)")
    void shouldAllowCreation_WhenCreateWithNegativeConfidence() {
        // given
        SleepStage stage = SleepStage.REM;
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        Double negativeConfidence = -0.5;

        // when
        SleepStageData stageData = SleepStageData.create(stage, startTime, negativeConfidence);

        // then
        assertThat(stageData).isNotNull();
        assertThat(stageData.getSleepStage()).isEqualTo(stage);
        assertThat(stageData.getStageStartTime()).isEqualTo(startTime);
        assertThat(stageData.getConfidenceScore()).isEqualTo(negativeConfidence);
        assertThat(stageData.getMovementCountDuringStage()).isEqualTo(0);
    }

    @Test
    @DisplayName("create - 1.0을 초과하는 confidence 값으로 생성 시 정상 처리")
    void shouldAllowCreation_WhenCreateWithConfidenceAboveOne() {
        // given
        SleepStage stage = SleepStage.LIGHT_SLEEP;
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        Double highConfidence = 1.5;

        // when
        SleepStageData stageData = SleepStageData.create(stage, startTime, highConfidence);

        // then
        assertThat(stageData).isNotNull();
        assertThat(stageData.getSleepStage()).isEqualTo(stage);
        assertThat(stageData.getStageStartTime()).isEqualTo(startTime);
        assertThat(stageData.getConfidenceScore()).isEqualTo(highConfidence);
        assertThat(stageData.getMovementCountDuringStage()).isEqualTo(0);
    }

    @Test
    @DisplayName("create - 정상적인 파라미터로 생성 시 올바른 초기화")
    void shouldCreateCorrectly_WhenValidParametersProvided() {
        // given
        SleepStage stage = SleepStage.DEEP_SLEEP;
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        Double confidence = 0.85;

        // when
        SleepStageData stageData = SleepStageData.create(stage, startTime, confidence);

        // then
        assertThat(stageData).isNotNull();
        assertThat(stageData.getSleepStage()).isEqualTo(stage);
        assertThat(stageData.getStageStartTime()).isEqualTo(startTime);
        assertThat(stageData.getConfidenceScore()).isEqualTo(confidence);
        assertThat(stageData.getMovementCountDuringStage()).isEqualTo(0);
        assertThat(stageData.getStageEndTime()).isNull();
        assertThat(stageData.getDurationMinutes()).isNull();
        assertThat(stageData.isInProgress()).isTrue();
    }

    @Test
    @DisplayName("create - 극값 confidence 처리 (0.0, 1.0)")
    void shouldHandleEdgeCaseConfidenceValues() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        
        // when
        SleepStageData zeroConfidenceStage = SleepStageData.create(SleepStage.AWAKE, startTime, 0.0);
        SleepStageData perfectConfidenceStage = SleepStageData.create(SleepStage.DEEP_SLEEP, startTime, 1.0);

        // then
        assertThat(zeroConfidenceStage.getConfidenceScore()).isEqualTo(0.0);
        assertThat(perfectConfidenceStage.getConfidenceScore()).isEqualTo(1.0);
    }

    // ========================== 통합 테스트 ==========================

    @Test
    @DisplayName("통합 테스트 - 수면 단계 생성부터 종료까지 전체 생명주기")
    void shouldHandleCompleteLifecycle_FromCreationToCompletion() {
        // given
        SleepStage stage = SleepStage.LIGHT_SLEEP;
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 22, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 23, 45);
        Double confidence = 0.75;

        // when - 생성
        SleepStageData stageData = SleepStageData.create(stage, startTime, confidence);
        
        // 초기 상태 확인
        assertThat(stageData.isInProgress()).isTrue();
        assertThat(stageData.isDeepSleep()).isFalse();
        assertThat(stageData.isREM()).isFalse();
        assertThat(stageData.getMovementCountDuringStage()).isEqualTo(0);

        // 움직임 카운트 증가
        stageData.incrementMovementCount();
        stageData.incrementMovementCount();
        stageData.incrementMovementCount();

        // 단계 종료
        stageData.endStage(endTime);

        // then - 최종 상태 확인
        assertThat(stageData.getSleepStage()).isEqualTo(stage);
        assertThat(stageData.getStageStartTime()).isEqualTo(startTime);
        assertThat(stageData.getStageEndTime()).isEqualTo(endTime);
        assertThat(stageData.getConfidenceScore()).isEqualTo(confidence);
        assertThat(stageData.getDurationMinutes()).isEqualTo(105); // 1시간 45분
        assertThat(stageData.getMovementCountDuringStage()).isEqualTo(3);
        assertThat(stageData.isInProgress()).isFalse();
        assertThat(stageData.isDeepSleep()).isFalse();
        assertThat(stageData.isREM()).isFalse();
    }

    @Test
    @DisplayName("통합 테스트 - DEEP_SLEEP 단계의 전체 동작 검증")
    void shouldHandleDeepSleepStageCorrectly() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 23, 30);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 2, 2, 0);
        
        // when
        SleepStageData deepSleepStage = SleepStageData.create(SleepStage.DEEP_SLEEP, startTime, 0.95);
        deepSleepStage.incrementMovementCount(); // 깊은 잠에서는 움직임이 적어야 함
        deepSleepStage.endStage(endTime);

        // then
        assertThat(deepSleepStage.isDeepSleep()).isTrue();
        assertThat(deepSleepStage.isREM()).isFalse();
        assertThat(deepSleepStage.getDurationMinutes()).isEqualTo(150); // 2시간 30분
        assertThat(deepSleepStage.getMovementCountDuringStage()).isEqualTo(1);
        assertThat(deepSleepStage.getConfidenceScore()).isEqualTo(0.95);
    }

    @Test
    @DisplayName("통합 테스트 - REM 수면 단계의 전체 동작 검증")
    void shouldHandleREMStageCorrectly() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 2, 5, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 2, 5, 45);
        
        // when
        SleepStageData remStage = SleepStageData.create(SleepStage.REM, startTime, 0.8);
        
        // REM 수면에서는 뇌 활동이 활발하여 약간의 움직임이 있을 수 있음
        for (int i = 0; i < 5; i++) {
            remStage.incrementMovementCount();
        }
        
        remStage.endStage(endTime);

        // then
        assertThat(remStage.isREM()).isTrue();
        assertThat(remStage.isDeepSleep()).isFalse();
        assertThat(remStage.getDurationMinutes()).isEqualTo(45);
        assertThat(remStage.getMovementCountDuringStage()).isEqualTo(5);
        assertThat(remStage.getConfidenceScore()).isEqualTo(0.8);
    }
}