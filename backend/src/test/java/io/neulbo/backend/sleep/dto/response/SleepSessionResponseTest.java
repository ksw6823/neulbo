package io.neulbo.backend.sleep.dto.response;

import io.neulbo.backend.sleep.domain.SleepQuality;
import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SleepStageData;
import io.neulbo.backend.sleep.domain.SessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SleepSessionResponseTest {

    @Test
    @DisplayName("SleepSession의 sleepStages가 null일 때 빈 리스트로 초기화")
    void shouldInitializeEmptyList_WhenSleepStagesIsNull() {
        // given
        SleepSession mockSession = createMockSleepSession();
        when(mockSession.getSleepStages()).thenReturn(null); // null 반환

        // when
        SleepSessionResponse response = new SleepSessionResponse(mockSession);

        // then
        assertThat(response.getSleepStages()).isNotNull();
        assertThat(response.getSleepStages()).isEmpty();
        assertThat(response.getSleepStages()).isEqualTo(Collections.emptyList());
    }

    @Test
    @DisplayName("SleepSession의 sleepStages가 빈 리스트일 때 정상 처리")
    void shouldHandleEmptyList_WhenSleepStagesIsEmpty() {
        // given
        SleepSession mockSession = createMockSleepSession();
        when(mockSession.getSleepStages()).thenReturn(Collections.emptyList());

        // when
        SleepSessionResponse response = new SleepSessionResponse(mockSession);

        // then
        assertThat(response.getSleepStages()).isNotNull();
        assertThat(response.getSleepStages()).isEmpty();
    }

    @Test
    @DisplayName("SleepSession의 sleepStages가 유효한 리스트일 때 정상 매핑")
    void shouldMapCorrectly_WhenSleepStagesIsValid() {
        // given
        SleepSession mockSession = createMockSleepSession();
        SleepStageData mockStageData1 = createMockSleepStageData();
        SleepStageData mockStageData2 = createMockSleepStageData();
        List<SleepStageData> sleepStages = List.of(mockStageData1, mockStageData2);
        
        when(mockSession.getSleepStages()).thenReturn(sleepStages);

        // when
        SleepSessionResponse response = new SleepSessionResponse(mockSession);

        // then
        assertThat(response.getSleepStages()).isNotNull();
        assertThat(response.getSleepStages()).hasSize(2);
        assertThat(response.getSleepStages()).allMatch(stage -> stage instanceof SleepStageResponse);
    }

    @Test
    @DisplayName("createSimple 정적 메서드에서는 sleepStages가 null로 설정")
    void shouldSetSleepStagesToNull_WhenUsingCreateSimple() {
        // given
        SleepSession mockSession = createMockSleepSession();
        SleepStageData mockStageData = createMockSleepStageData();
        when(mockSession.getSleepStages()).thenReturn(List.of(mockStageData));

        // when
        SleepSessionResponse response = SleepSessionResponse.createSimple(mockSession);

        // then
        assertThat(response.getSleepStages()).isNull(); // createSimple에서는 의도적으로 null
    }

    @Test
    @DisplayName("null 체크 없이 호출했을 때 NullPointerException이 발생하지 않음")
    void shouldNotThrowNullPointerException_WhenCalledWithNullSleepStages() {
        // given
        SleepSession mockSession = createMockSleepSession();
        when(mockSession.getSleepStages()).thenReturn(null);

        // when & then
        assertThatCode(() -> new SleepSessionResponse(mockSession))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("모든 필드가 올바르게 매핑되는지 확인")
    void shouldMapAllFieldsCorrectly() {
        // given
        SleepSession mockSession = createMockSleepSession();
        when(mockSession.getSleepStages()).thenReturn(Collections.emptyList());

        // when
        SleepSessionResponse response = new SleepSessionResponse(mockSession);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSleepStartTime()).isNotNull();
        assertThat(response.getSleepEndTime()).isNotNull();
        assertThat(response.getIntendedSleepDurationMinutes()).isEqualTo(480);
        assertThat(response.getActualSleepDurationMinutes()).isEqualTo(450);
        assertThat(response.getSleepEfficiencyPercentage()).isEqualTo(85.0);
        assertThat(response.getTotalMovementCount()).isEqualTo(10);
        assertThat(response.getWakeUpCount()).isEqualTo(2);
        assertThat(response.getSleepQuality()).isEqualTo(SleepQuality.GOOD);
        assertThat(response.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(response.getNotes()).isEqualTo("테스트 수면 세션");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getSleepStages()).isNotNull(); // null이 아닌 빈 리스트
    }

    private SleepSession createMockSleepSession() {
        SleepSession mockSession = Mockito.mock(SleepSession.class);
        
        when(mockSession.getId()).thenReturn(1L);
        when(mockSession.getSleepStartTime()).thenReturn(LocalDateTime.now().minusHours(8));
        when(mockSession.getSleepEndTime()).thenReturn(LocalDateTime.now());
        when(mockSession.getIntendedSleepDurationMinutes()).thenReturn(480);
        when(mockSession.getActualSleepDurationMinutes()).thenReturn(450);
        when(mockSession.getSleepEfficiencyPercentage()).thenReturn(85.0);
        when(mockSession.getTotalMovementCount()).thenReturn(10);
        when(mockSession.getWakeUpCount()).thenReturn(2);
        when(mockSession.getSleepQuality()).thenReturn(SleepQuality.GOOD);
        when(mockSession.getSessionStatus()).thenReturn(SessionStatus.COMPLETED);
        when(mockSession.getNotes()).thenReturn("테스트 수면 세션");
        when(mockSession.getCreatedAt()).thenReturn(LocalDateTime.now());
        
        return mockSession;
    }

    private SleepStageData createMockSleepStageData() {
        SleepStageData mockStageData = Mockito.mock(SleepStageData.class);
        
        when(mockStageData.getId()).thenReturn(1L);
        when(mockStageData.getStageStartTime()).thenReturn(LocalDateTime.now().minusHours(1));
        when(mockStageData.getStageEndTime()).thenReturn(LocalDateTime.now());
        when(mockStageData.getDurationMinutes()).thenReturn(60);
        
        return mockStageData;
    }
}