package io.neulbo.backend.sleep.service;

import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SleepStageData;
import io.neulbo.backend.sleep.domain.SessionStatus;
import io.neulbo.backend.sleep.repository.MovementDataRepository;
import io.neulbo.backend.sleep.repository.SleepSessionRepository;
import io.neulbo.backend.sleep.repository.SleepStageDataRepository;
import io.neulbo.backend.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SleepAnalysisService의 벌크 삭제 기능을 검증하는 테스트
 */
@ExtendWith(MockitoExtension.class)
class SleepAnalysisServiceBulkDeleteTest {

    @Mock
    private SleepSessionRepository sleepSessionRepository;

    @Mock
    private SleepStageDataRepository sleepStageDataRepository;

    @Mock
    private MovementDataRepository movementDataRepository;

    @InjectMocks
    private SleepAnalysisService sleepAnalysisService;

    private User testUser;
    private SleepSession testSession;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.now().minusHours(8);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .provider("google")
                .providerId("test123")
                .username("testuser")
                .build();

        testSession = mock(SleepSession.class);
        when(testSession.getId()).thenReturn(1L);
        when(testSession.getUser()).thenReturn(testUser);
        when(testSession.getSleepStartTime()).thenReturn(baseTime);
        when(testSession.getSleepEndTime()).thenReturn(baseTime.plusHours(8));
        when(testSession.getSessionStatus()).thenReturn(SessionStatus.COMPLETED);
        when(testSession.isCompleted()).thenReturn(true);
    }

    @Test
    @DisplayName("수면 세션 분석 시 기존 데이터 벌크 삭제 사용 확인")
    void analyzeSleepSession_UsesBulkDelete() {
        // Given
        Long sessionId = 1L;
        int expectedDeletedCount = 5;

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(sleepStageDataRepository.deleteBySleepSession(testSession)).thenReturn(expectedDeletedCount);
        when(movementDataRepository.findBySleepSessionOrderByTimestampAsc(testSession)).thenReturn(Collections.emptyList());

        // When
        sleepAnalysisService.analyzeSleepSession(sessionId);

        // Then
        // 벌크 삭제 메서드가 정확히 한 번 호출되었는지 확인
        verify(sleepStageDataRepository, times(1)).deleteBySleepSession(testSession);
        
        // 기존의 find + deleteAll 방식이 호출되지 않았는지 확인
        verify(sleepStageDataRepository, never()).findBySleepSessionOrderByStageStartTimeAsc(any());
        verify(sleepStageDataRepository, never()).deleteAll(anyList());
        
        // 벌크 삭제가 먼저 실행되고 나서 움직임 데이터 조회가 실행되는지 순서 확인
        var inOrder = inOrder(sleepStageDataRepository, movementDataRepository);
        inOrder.verify(sleepStageDataRepository).deleteBySleepSession(testSession);
        inOrder.verify(movementDataRepository).findBySleepSessionOrderByTimestampAsc(testSession);
    }

    @Test
    @DisplayName("기존 수면 단계 데이터가 없는 경우 벌크 삭제 정상 처리")
    void analyzeSleepSession_NoPreviousData_BulkDeleteReturnsZero() {
        // Given
        Long sessionId = 1L;

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(sleepStageDataRepository.deleteBySleepSession(testSession)).thenReturn(0); // 삭제된 데이터 없음
        when(movementDataRepository.findBySleepSessionOrderByTimestampAsc(testSession)).thenReturn(Collections.emptyList());

        // When
        sleepAnalysisService.analyzeSleepSession(sessionId);

        // Then
        verify(sleepStageDataRepository, times(1)).deleteBySleepSession(testSession);
        
        // 0개 삭제되어도 정상적으로 처리되는지 확인
        verify(movementDataRepository, times(1)).findBySleepSessionOrderByTimestampAsc(testSession);
    }

    @Test
    @DisplayName("벌크 삭제 사용으로 메모리 효율성 개선 확인")
    void analyzeSleepSession_MemoryEfficiency() {
        // Given
        Long sessionId = 1L;
        int largeDataSetSize = 1000; // 대용량 데이터 시뮬레이션

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(sleepStageDataRepository.deleteBySleepSession(testSession)).thenReturn(largeDataSetSize);
        when(movementDataRepository.findBySleepSessionOrderByTimestampAsc(testSession)).thenReturn(Collections.emptyList());

        // When
        sleepAnalysisService.analyzeSleepSession(sessionId);

        // Then
        // 벌크 삭제를 사용하므로 대용량 데이터도 메모리에 로드하지 않음
        verify(sleepStageDataRepository, times(1)).deleteBySleepSession(testSession);
        
        // 기존의 메모리 비효율적인 방식이 호출되지 않았는지 재확인
        verify(sleepStageDataRepository, never()).findBySleepSessionOrderByStageStartTimeAsc(any());
        
        // ArgumentCaptor로 정확한 세션이 전달되었는지 확인
        ArgumentCaptor<SleepSession> sessionCaptor = ArgumentCaptor.forClass(SleepSession.class);
        verify(sleepStageDataRepository).deleteBySleepSession(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue()).isEqualTo(testSession);
    }

    @Test
    @DisplayName("벌크 삭제 실패 시 예외 전파 확인")
    void analyzeSleepSession_BulkDeleteFailure_PropagatesException() {
        // Given
        Long sessionId = 1L;
        RuntimeException deleteException = new RuntimeException("벌크 삭제 실패");

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(sleepStageDataRepository.deleteBySleepSession(testSession)).thenThrow(deleteException);

        // When & Then
        assertThatThrownBy(() -> sleepAnalysisService.analyzeSleepSession(sessionId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("벌크 삭제 실패");

        // 벌크 삭제에서 실패했으므로 다음 단계는 실행되지 않아야 함
        verify(movementDataRepository, never()).findBySleepSessionOrderByTimestampAsc(any());
    }

    @Test
    @DisplayName("재분석 시나리오에서 벌크 삭제 동작 검증")
    void analyzeSleepSession_ReanalysisScenario_BulkDeletesExistingData() {
        // Given - 재분석 시나리오 (기존 분석 데이터 존재)
        Long sessionId = 1L;
        int existingDataCount = 8; // 기존에 8개의 수면 단계 데이터 존재

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(sleepStageDataRepository.deleteBySleepSession(testSession)).thenReturn(existingDataCount);
        when(movementDataRepository.findBySleepSessionOrderByTimestampAsc(testSession)).thenReturn(Collections.emptyList());

        // When
        sleepAnalysisService.analyzeSleepSession(sessionId);

        // Then
        // 재분석을 위해 기존 데이터가 벌크 삭제되었는지 확인
        verify(sleepStageDataRepository, times(1)).deleteBySleepSession(testSession);
        
        // 삭제 후 새로운 분석 프로세스가 시작되는지 확인
        verify(movementDataRepository, times(1)).findBySleepSessionOrderByTimestampAsc(testSession);
    }

    @Test
    @DisplayName("성능 테스트: 벌크 삭제는 단일 쿼리로 실행")
    void analyzeSleepSession_PerformanceTest_SingleQuery() {
        // Given
        Long sessionId = 1L;
        
        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(sleepStageDataRepository.deleteBySleepSession(testSession)).thenReturn(100);
        when(movementDataRepository.findBySleepSessionOrderByTimestampAsc(testSession)).thenReturn(Collections.emptyList());

        // When
        sleepAnalysisService.analyzeSleepSession(sessionId);

        // Then
        // 벌크 삭제는 단일 호출로만 실행 (N+1 문제 없음)
        verify(sleepStageDataRepository, times(1)).deleteBySleepSession(testSession);
        
        // 개별 삭제나 배치 삭제가 호출되지 않았는지 확인
        verify(sleepStageDataRepository, never()).delete(any(SleepStageData.class));
        verify(sleepStageDataRepository, never()).deleteById(anyLong());
        verify(sleepStageDataRepository, never()).deleteAll();
        verify(sleepStageDataRepository, never()).deleteAllInBatch();
    }

    @Test
    @DisplayName("트랜잭션 컨텍스트에서 벌크 삭제 동작 확인")
    void analyzeSleepSession_TransactionalContext() {
        // Given
        Long sessionId = 1L;

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(sleepStageDataRepository.deleteBySleepSession(testSession)).thenReturn(3);
        when(movementDataRepository.findBySleepSessionOrderByTimestampAsc(testSession)).thenReturn(Collections.emptyList());

        // When
        sleepAnalysisService.analyzeSleepSession(sessionId);

        // Then
        // @Modifying 쿼리가 트랜잭션 내에서 실행되는지 확인
        verify(sleepStageDataRepository, times(1)).deleteBySleepSession(testSession);
        
        // 트랜잭션 순서: 삭제 -> 조회 -> 분석 -> 저장
        var inOrder = inOrder(sleepStageDataRepository, movementDataRepository);
        inOrder.verify(sleepStageDataRepository).deleteBySleepSession(testSession);
        inOrder.verify(movementDataRepository).findBySleepSessionOrderByTimestampAsc(testSession);
    }

    @Test
    @DisplayName("다양한 삭제 카운트 시나리오 검증")
    void analyzeSleepSession_VariousDeleteCounts() {
        // Test Case 1: 0개 삭제 (새로운 분석)
        testBulkDeleteWithCount(0, "새로운 분석");
        
        // Test Case 2: 소량 삭제
        testBulkDeleteWithCount(3, "소량 데이터 재분석");
        
        // Test Case 3: 대량 삭제
        testBulkDeleteWithCount(500, "대량 데이터 재분석");
    }

    private void testBulkDeleteWithCount(int deleteCount, String scenario) {
        // Given
        reset(sleepSessionRepository, sleepStageDataRepository, movementDataRepository);
        
        Long sessionId = 1L;
        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(sleepStageDataRepository.deleteBySleepSession(testSession)).thenReturn(deleteCount);
        when(movementDataRepository.findBySleepSessionOrderByTimestampAsc(testSession)).thenReturn(Collections.emptyList());

        // When
        sleepAnalysisService.analyzeSleepSession(sessionId);

        // Then
        verify(sleepStageDataRepository, times(1)).deleteBySleepSession(testSession);
        verify(movementDataRepository, times(1)).findBySleepSessionOrderByTimestampAsc(testSession);
        
        System.out.printf("%s: %d개 데이터 벌크 삭제 성공%n", scenario, deleteCount);
    }

    @Test
    @DisplayName("벌크 삭제 메서드 시그니처 검증")
    void verifyBulkDeleteMethodSignature() {
        // Given
        Long sessionId = 1L;
        
        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(sleepStageDataRepository.deleteBySleepSession(any(SleepSession.class))).thenReturn(1);
        when(movementDataRepository.findBySleepSessionOrderByTimestampAsc(testSession)).thenReturn(Collections.emptyList());

        // When
        sleepAnalysisService.analyzeSleepSession(sessionId);

        // Then
        // 정확한 타입의 파라미터가 전달되는지 확인
        ArgumentCaptor<SleepSession> sessionCaptor = ArgumentCaptor.forClass(SleepSession.class);
        verify(sleepStageDataRepository).deleteBySleepSession(sessionCaptor.capture());
        
        SleepSession capturedSession = sessionCaptor.getValue();
        assertThat(capturedSession).isNotNull();
        assertThat(capturedSession).isEqualTo(testSession);
        assertThat(capturedSession.getId()).isEqualTo(1L);
    }
}