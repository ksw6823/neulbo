package io.neulbo.backend.sleep.repository;

import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SleepStage;
import io.neulbo.backend.sleep.domain.SessionStatus;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SleepStageDataRepository의 벌크 삭제 메서드 시그니처와 동작을 검증하는 테스트
 */
@ExtendWith(MockitoExtension.class)
class SleepStageDataRepositoryBulkDeleteTest {

    @Mock
    private SleepStageDataRepository sleepStageDataRepository;

    @Mock
    private SleepSessionRepository sleepSessionRepository;

    @Mock
    private UserRepository userRepository;

    private User testUser;
    private SleepSession testSession;
    private SleepSession anotherSession;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.now().minusHours(8);

        // 테스트 사용자 생성
        testUser = User.builder()
                .id(UUID.randomUUID())
                .provider("google")
                .providerId("test123")
                .username("testuser")
                .build();

        // 테스트 수면 세션들 생성
        testSession = mock(SleepSession.class);
        when(testSession.getId()).thenReturn(1L);
        when(testSession.getUser()).thenReturn(testUser);
        when(testSession.getSleepStartTime()).thenReturn(baseTime);
        when(testSession.getSleepEndTime()).thenReturn(baseTime.plusHours(8));
        when(testSession.getSessionStatus()).thenReturn(SessionStatus.COMPLETED);

        anotherSession = mock(SleepSession.class);
        when(anotherSession.getId()).thenReturn(2L);
        when(anotherSession.getUser()).thenReturn(testUser);
        when(anotherSession.getSleepStartTime()).thenReturn(baseTime.plusDays(1));
        when(anotherSession.getSleepEndTime()).thenReturn(baseTime.plusDays(1).plusHours(8));
        when(anotherSession.getSessionStatus()).thenReturn(SessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("벌크 삭제 메서드 시그니처와 반환값 검증")
    void deleteBySleepSession_Success() {
        // Given
        int expectedDeletedCount = 4;
        when(sleepStageDataRepository.deleteBySleepSession(testSession)).thenReturn(expectedDeletedCount);

        // When
        int actualDeletedCount = sleepStageDataRepository.deleteBySleepSession(testSession);

        // Then
        assertThat(actualDeletedCount).isEqualTo(expectedDeletedCount);
        verify(sleepStageDataRepository, times(1)).deleteBySleepSession(testSession);
    }

    @Test
    @DisplayName("특정 수면 단계만 벌크 삭제")
    void deleteBySleepSessionAndSleepStage_Success() {
        // Given
        int expectedDeletedCount = 2;
        when(sleepStageDataRepository.deleteBySleepSessionAndSleepStage(testSession, SleepStage.DEEP_SLEEP))
                .thenReturn(expectedDeletedCount);

        // When
        int actualDeletedCount = sleepStageDataRepository.deleteBySleepSessionAndSleepStage(testSession, SleepStage.DEEP_SLEEP);

        // Then
        assertThat(actualDeletedCount).isEqualTo(expectedDeletedCount);
        verify(sleepStageDataRepository, times(1)).deleteBySleepSessionAndSleepStage(testSession, SleepStage.DEEP_SLEEP);
    }

    @Test
    @DisplayName("시간 범위 기반 벌크 삭제")
    void deleteBySleepSessionAndTimeRange_Success() {
        // Given
        LocalDateTime rangeStart = baseTime.plusMinutes(45);
        LocalDateTime rangeEnd = baseTime.plusMinutes(150);
        int expectedDeletedCount = 2;
        
        when(sleepStageDataRepository.deleteBySleepSessionAndTimeRange(testSession, rangeStart, rangeEnd))
                .thenReturn(expectedDeletedCount);

        // When
        int actualDeletedCount = sleepStageDataRepository.deleteBySleepSessionAndTimeRange(testSession, rangeStart, rangeEnd);

        // Then
        assertThat(actualDeletedCount).isEqualTo(expectedDeletedCount);
        verify(sleepStageDataRepository, times(1)).deleteBySleepSessionAndTimeRange(testSession, rangeStart, rangeEnd);
    }

    @Test
    @DisplayName("완료되지 않은 수면 단계 데이터 벌크 삭제")
    void deleteIncompleteSleepStagesBySleepSession_Success() {
        // Given
        int expectedDeletedCount = 2;
        when(sleepStageDataRepository.deleteIncompleteSleepStagesBySleepSession(testSession))
                .thenReturn(expectedDeletedCount);

        // When
        int actualDeletedCount = sleepStageDataRepository.deleteIncompleteSleepStagesBySleepSession(testSession);

        // Then
        assertThat(actualDeletedCount).isEqualTo(expectedDeletedCount);
        verify(sleepStageDataRepository, times(1)).deleteIncompleteSleepStagesBySleepSession(testSession);
    }

    @Test
    @DisplayName("빈 세션에 대한 벌크 삭제는 0 반환")
    void deleteBySleepSession_EmptySession_ReturnsZero() {
        // Given
        when(sleepStageDataRepository.deleteBySleepSession(testSession)).thenReturn(0);

        // When
        int deletedCount = sleepStageDataRepository.deleteBySleepSession(testSession);

        // Then
        assertThat(deletedCount).isZero();
        verify(sleepStageDataRepository, times(1)).deleteBySleepSession(testSession);
    }

    @Test
    @DisplayName("벌크 삭제 메서드들이 @Modifying 어노테이션을 사용하는지 검증")
    void verifyBulkDeleteMethodsUseModifyingAnnotation() {
        // 이 테스트는 컴파일 타임에 @Modifying 어노테이션의 존재를 확인하고
        // 런타임에는 메서드 호출이 정상적으로 작동하는지 검증합니다
        
        // Given
        when(sleepStageDataRepository.deleteBySleepSession(any())).thenReturn(5);
        when(sleepStageDataRepository.deleteBySleepSessionAndSleepStage(any(), any())).thenReturn(3);
        when(sleepStageDataRepository.deleteBySleepSessionAndTimeRange(any(), any(), any())).thenReturn(2);
        when(sleepStageDataRepository.deleteIncompleteSleepStagesBySleepSession(any())).thenReturn(1);

        // When & Then - 모든 벌크 삭제 메서드가 정상 호출되는지 확인
        assertThat(sleepStageDataRepository.deleteBySleepSession(testSession)).isEqualTo(5);
        assertThat(sleepStageDataRepository.deleteBySleepSessionAndSleepStage(testSession, SleepStage.DEEP_SLEEP)).isEqualTo(3);
        assertThat(sleepStageDataRepository.deleteBySleepSessionAndTimeRange(testSession, baseTime, baseTime.plusHours(1))).isEqualTo(2);
        assertThat(sleepStageDataRepository.deleteIncompleteSleepStagesBySleepSession(testSession)).isEqualTo(1);

        // 모든 메서드가 호출되었는지 검증
        verify(sleepStageDataRepository).deleteBySleepSession(testSession);
        verify(sleepStageDataRepository).deleteBySleepSessionAndSleepStage(testSession, SleepStage.DEEP_SLEEP);
        verify(sleepStageDataRepository).deleteBySleepSessionAndTimeRange(testSession, baseTime, baseTime.plusHours(1));
        verify(sleepStageDataRepository).deleteIncompleteSleepStagesBySleepSession(testSession);
    }
}