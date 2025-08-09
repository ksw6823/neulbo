package io.neulbo.backend.sleep.service;

import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SessionStatus;
import io.neulbo.backend.sleep.dto.response.SleepStatisticsResponse;
import io.neulbo.backend.sleep.repository.MovementDataRepository;
import io.neulbo.backend.sleep.repository.SleepSessionRepository;
import io.neulbo.backend.sleep.repository.SleepStageDataRepository;
import io.neulbo.backend.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SleepAnalysisService의 수면 통계 기능에서 전체 세션 수와 완료된 세션 수가 
 * 올바르게 구분되어 설정되는지 검증하는 테스트
 */
@ExtendWith(MockitoExtension.class)
class SleepAnalysisServiceStatisticsTest {

    @Mock
    private SleepSessionRepository sleepSessionRepository;

    @Mock
    private SleepStageDataRepository sleepStageDataRepository;

    @Mock
    private MovementDataRepository movementDataRepository;

    @InjectMocks
    private SleepAnalysisService sleepAnalysisService;

    private User testUser;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.now().minusDays(7);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .provider("google")
                .providerId("test123")
                .username("testuser")
                .build();
    }

    private SleepSession createMockSleepSession(Long id, SessionStatus status, LocalDateTime startTime) {
        SleepSession session = mock(SleepSession.class);
        when(session.getId()).thenReturn(id);
        when(session.getUser()).thenReturn(testUser);
        when(session.getSessionStatus()).thenReturn(status);
        when(session.getSleepStartTime()).thenReturn(startTime);
        when(session.getSleepEndTime()).thenReturn(startTime.plusHours(8));
        when(session.isCompleted()).thenReturn(status == SessionStatus.COMPLETED);
        when(session.getActualSleepDurationMinutes()).thenReturn(480); // 8시간
        when(session.getSleepEfficiencyPercentage()).thenReturn(85.0);
        when(session.getWakeUpCount()).thenReturn(2);
        when(session.getTotalMovementCount()).thenReturn(15);
        when(session.getSleepStages()).thenReturn(Collections.emptyList());
        return session;
    }

    @Test
    @DisplayName("전체 수면 통계에서 전체 세션 수와 완료된 세션 수가 올바르게 구분됨")
    void getSleepStatistics_CorrectlyDistinguishesTotalAndCompletedSessions() {
        // Given
        SleepSession completedSession1 = createMockSleepSession(1L, SessionStatus.COMPLETED, baseTime);
        SleepSession completedSession2 = createMockSleepSession(2L, SessionStatus.COMPLETED, baseTime.plusDays(1));
        // 진행중인 세션과 취소된 세션도 전체 카운트에 포함됨

        List<SleepSession> completedSessions = Arrays.asList(completedSession1, completedSession2);

        // Mock repository 응답 설정
        when(sleepSessionRepository.findByUserAndSessionStatusOrderByCreatedAtDesc(testUser, SessionStatus.COMPLETED))
                .thenReturn(completedSessions);
        when(sleepSessionRepository.countByUser(testUser)).thenReturn(4L); // 전체 4개 세션
        when(sleepSessionRepository.findAverageSleepDurationByUser(testUser)).thenReturn(Optional.of(480.0));
        when(sleepSessionRepository.findAverageSleepEfficiencyByUser(testUser)).thenReturn(Optional.of(85.0));

        // When
        SleepStatisticsResponse response = sleepAnalysisService.getSleepStatistics(testUser);

        // Then
        assertThat(response.getTotalSleepSessions()).isEqualTo(4); // 전체 세션 수 (완료 + 진행중 + 취소됨)
        assertThat(response.getCompletedSessions()).isEqualTo(2); // 완료된 세션 수만

        // 전체 세션 수와 완료된 세션 수가 다름을 확인
        assertThat(response.getTotalSleepSessions()).isNotEqualTo(response.getCompletedSessions());

        // Repository 호출 확인
        verify(sleepSessionRepository).findByUserAndSessionStatusOrderByCreatedAtDesc(testUser, SessionStatus.COMPLETED);
        verify(sleepSessionRepository).countByUser(testUser);
    }

    @Test
    @DisplayName("완료된 세션이 없을 때 빈 통계 반환")
    void getSleepStatistics_EmptyStatisticsWhenNoCompletedSessions() {
        // Given
        when(sleepSessionRepository.findByUserAndSessionStatusOrderByCreatedAtDesc(testUser, SessionStatus.COMPLETED))
                .thenReturn(Collections.emptyList());

        // When
        SleepStatisticsResponse response = sleepAnalysisService.getSleepStatistics(testUser);

        // Then
        assertThat(response.getTotalSleepSessions()).isEqualTo(0);
        assertThat(response.getCompletedSessions()).isEqualTo(0);
        assertThat(response.getAverageSleepDurationMinutes()).isEqualTo(0.0);

        // countByUser는 호출되지 않아야 함 (완료된 세션이 없으므로)
        verify(sleepSessionRepository, never()).countByUser(any());
    }

    @Test
    @DisplayName("특정 기간 수면 통계에서 해당 기간의 전체 세션 수와 완료된 세션 수가 올바르게 구분됨")
    void getSleepStatisticsForPeriod_CorrectlyDistinguishesTotalAndCompletedSessions() {
        // Given
        LocalDateTime startDate = baseTime;
        LocalDateTime endDate = baseTime.plusDays(5);

        SleepSession completedSession1 = createMockSleepSession(1L, SessionStatus.COMPLETED, baseTime.plusDays(1));
        SleepSession completedSession2 = createMockSleepSession(2L, SessionStatus.COMPLETED, baseTime.plusDays(2));
        SleepSession inProgressSession = createMockSleepSession(3L, SessionStatus.IN_PROGRESS, baseTime.plusDays(3));

        // 해당 기간의 모든 세션 (완료 + 진행중)
        List<SleepSession> allSessionsInPeriod = Arrays.asList(completedSession1, completedSession2, inProgressSession);

        when(sleepSessionRepository.findByUserAndDateRange(testUser, startDate, endDate))
                .thenReturn(allSessionsInPeriod);

        // When
        SleepStatisticsResponse response = sleepAnalysisService.getSleepStatisticsForPeriod(testUser, startDate, endDate);

        // Then
        assertThat(response.getTotalSleepSessions()).isEqualTo(3); // 해당 기간의 전체 세션 수
        assertThat(response.getCompletedSessions()).isEqualTo(2); // 해당 기간의 완료된 세션 수

        // 전체 세션 수와 완료된 세션 수가 다름을 확인
        assertThat(response.getTotalSleepSessions()).isNotEqualTo(response.getCompletedSessions());

        verify(sleepSessionRepository).findByUserAndDateRange(testUser, startDate, endDate);
    }

    @Test
    @DisplayName("특정 기간에 완료된 세션이 없을 때 빈 통계 반환")
    void getSleepStatisticsForPeriod_EmptyStatisticsWhenNoCompletedSessions() {
        // Given
        LocalDateTime startDate = baseTime;
        LocalDateTime endDate = baseTime.plusDays(5);

        SleepSession inProgressSession = createMockSleepSession(1L, SessionStatus.IN_PROGRESS, baseTime.plusDays(1));
        List<SleepSession> allSessionsInPeriod = Arrays.asList(inProgressSession); // 진행중인 세션만 있음

        when(sleepSessionRepository.findByUserAndDateRange(testUser, startDate, endDate))
                .thenReturn(allSessionsInPeriod);

        // When
        SleepStatisticsResponse response = sleepAnalysisService.getSleepStatisticsForPeriod(testUser, startDate, endDate);

        // Then
        assertThat(response.getTotalSleepSessions()).isEqualTo(0);
        assertThat(response.getCompletedSessions()).isEqualTo(0);
        assertThat(response.getAverageSleepDurationMinutes()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("전체 세션 수가 완료된 세션 수와 같을 때도 올바르게 처리")
    void getSleepStatistics_WhenAllSessionsAreCompleted() {
        // Given - 모든 세션이 완료된 경우
        SleepSession completedSession1 = createMockSleepSession(1L, SessionStatus.COMPLETED, baseTime);
        SleepSession completedSession2 = createMockSleepSession(2L, SessionStatus.COMPLETED, baseTime.plusDays(1));

        List<SleepSession> completedSessions = Arrays.asList(completedSession1, completedSession2);

        when(sleepSessionRepository.findByUserAndSessionStatusOrderByCreatedAtDesc(testUser, SessionStatus.COMPLETED))
                .thenReturn(completedSessions);
        when(sleepSessionRepository.countByUser(testUser)).thenReturn(2L); // 전체 2개 세션 (모두 완료)
        when(sleepSessionRepository.findAverageSleepDurationByUser(testUser)).thenReturn(Optional.of(480.0));
        when(sleepSessionRepository.findAverageSleepEfficiencyByUser(testUser)).thenReturn(Optional.of(85.0));

        // When
        SleepStatisticsResponse response = sleepAnalysisService.getSleepStatistics(testUser);

        // Then
        assertThat(response.getTotalSleepSessions()).isEqualTo(2);
        assertThat(response.getCompletedSessions()).isEqualTo(2);
        
        // 이 경우에만 전체 세션 수와 완료된 세션 수가 같음
        assertThat(response.getTotalSleepSessions()).isEqualTo(response.getCompletedSessions());
    }

    @Test
    @DisplayName("대량의 세션이 있을 때 성능 테스트")
    void getSleepStatistics_PerformanceWithLargeSessions() {
        // Given
        int totalSessions = 1000;
        int completedSessions = 800;

        when(sleepSessionRepository.findByUserAndSessionStatusOrderByCreatedAtDesc(testUser, SessionStatus.COMPLETED))
                .thenReturn(Collections.nCopies(completedSessions, 
                    createMockSleepSession(1L, SessionStatus.COMPLETED, baseTime)));
        when(sleepSessionRepository.countByUser(testUser)).thenReturn((long) totalSessions);
        when(sleepSessionRepository.findAverageSleepDurationByUser(testUser)).thenReturn(Optional.of(480.0));
        when(sleepSessionRepository.findAverageSleepEfficiencyByUser(testUser)).thenReturn(Optional.of(85.0));

        // When
        long startTime = System.currentTimeMillis();
        SleepStatisticsResponse response = sleepAnalysisService.getSleepStatistics(testUser);
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(response.getTotalSleepSessions()).isEqualTo(totalSessions);
        assertThat(response.getCompletedSessions()).isEqualTo(completedSessions);
        
        // 환경별 동적 성능 검증
        long executionTime = endTime - startTime;
        long maxAllowedTime = getStatisticsPerformanceThreshold(completedSessions);
        
        assertThat(executionTime)
            .as("대량 세션 통계 처리 시간이 허용 범위(%dms) 내에 있어야 함. 실제: %dms", 
                maxAllowedTime, executionTime)
            .isLessThan(maxAllowedTime);

        System.out.printf("대량 세션 통계 처리: 전체 %d개, 완료 %d개를 %dms에 처리 (허용: %dms)%n", 
                totalSessions, completedSessions, executionTime, maxAllowedTime);
    }

    @Test
    @DisplayName("Repository 호출 최적화 확인")
    void getSleepStatistics_OptimizedRepositoryCalls() {
        // Given
        List<SleepSession> completedSessions = Arrays.asList(
            createMockSleepSession(1L, SessionStatus.COMPLETED, baseTime)
        );

        when(sleepSessionRepository.findByUserAndSessionStatusOrderByCreatedAtDesc(testUser, SessionStatus.COMPLETED))
                .thenReturn(completedSessions);
        when(sleepSessionRepository.countByUser(testUser)).thenReturn(3L);
        when(sleepSessionRepository.findAverageSleepDurationByUser(testUser)).thenReturn(Optional.of(480.0));
        when(sleepSessionRepository.findAverageSleepEfficiencyByUser(testUser)).thenReturn(Optional.of(85.0));

        // When
        sleepAnalysisService.getSleepStatistics(testUser);

        // Then - Repository 메서드들이 정확히 한 번씩만 호출되는지 확인
        verify(sleepSessionRepository, times(1)).findByUserAndSessionStatusOrderByCreatedAtDesc(testUser, SessionStatus.COMPLETED);
        verify(sleepSessionRepository, times(1)).countByUser(testUser);
        verify(sleepSessionRepository, times(1)).findAverageSleepDurationByUser(testUser);
        verify(sleepSessionRepository, times(1)).findAverageSleepEfficiencyByUser(testUser);

        // 불필요한 호출이 없는지 확인
        verify(sleepSessionRepository, never()).findByUserOrderByCreatedAtDesc(eq(testUser), any());
        verify(sleepSessionRepository, never()).findAll();
    }

    @Test
    @DisplayName("세션 상태별 분포 확인")
    void verifySessionStatusDistribution() {
        // 이 테스트는 수정 전후의 동작 차이를 명확히 보여줍니다
        
        // Given - 다양한 상태의 세션들 (완료 2개 + 기타 2개 = 총 4개)
        SleepSession completed1 = createMockSleepSession(1L, SessionStatus.COMPLETED, baseTime);
        SleepSession completed2 = createMockSleepSession(2L, SessionStatus.COMPLETED, baseTime.plusDays(1));
        // 전체 세션 수에는 진행중, 취소된 세션도 포함됨

        List<SleepSession> completedSessions = Arrays.asList(completed1, completed2);
        long totalSessions = 4L; // COMPLETED(2) + IN_PROGRESS(1) + CANCELLED(1)

        when(sleepSessionRepository.findByUserAndSessionStatusOrderByCreatedAtDesc(testUser, SessionStatus.COMPLETED))
                .thenReturn(completedSessions);
        when(sleepSessionRepository.countByUser(testUser)).thenReturn(totalSessions);
        when(sleepSessionRepository.findAverageSleepDurationByUser(testUser)).thenReturn(Optional.of(480.0));
        when(sleepSessionRepository.findAverageSleepEfficiencyByUser(testUser)).thenReturn(Optional.of(85.0));

        // When
        SleepStatisticsResponse response = sleepAnalysisService.getSleepStatistics(testUser);

        // Then - 수정 후: 전체 세션 수 != 완료된 세션 수
        assertThat(response.getTotalSleepSessions()).isEqualTo(4); // 수정 전에는 2였음 (잘못된 값)
        assertThat(response.getCompletedSessions()).isEqualTo(2);

        // 완료율 계산 가능
        double completionRate = (double) response.getCompletedSessions() / response.getTotalSleepSessions() * 100;
        assertThat(completionRate).isEqualTo(50.0); // 4개 중 2개 완료 = 50%

        System.out.printf("세션 상태 분포: 전체 %d개, 완료 %d개, 완료율 %.1f%%%n", 
                response.getTotalSleepSessions(), response.getCompletedSessions(), completionRate);
    }

    /**
     * 통계 처리 성능 임계값 계산
     * 세션 수에 따른 동적 성능 기준 설정
     * 
     * @param sessionCount 처리할 세션 수
     * @return 허용 가능한 최대 처리 시간 (ms)
     */
    private long getStatisticsPerformanceThreshold(int sessionCount) {
        // 환경 변수로 통계 성능 배율 조정 가능 (기본값: 2.0)
        double statisticsMultiplier = Double.parseDouble(
            System.getProperty("test.statistics.multiplier", "2.0"));
        
        // CI 환경 감지
        boolean isCiEnvironment = System.getenv("CI") != null || 
                                 System.getenv("CONTINUOUS_INTEGRATION") != null ||
                                 System.getProperty("test.environment", "").equals("ci");
        
        // CI 환경에서는 더 관대한 임계값 적용
        if (isCiEnvironment) {
            statisticsMultiplier *= 3.0; // CI에서는 3배 더 관대하게
        }
        
        // 기본 통계 처리 시간: 세션 수에 따라 조정
        // 1000개 세션 기준으로 100ms 예상, 선형적으로 확장
        long baseTime = Math.max(sessionCount / 10, 50); // 최소 50ms
        
        // 최종 임계값 계산
        long threshold = Math.max((long) (baseTime * statisticsMultiplier), 500L);
        
        // 안전장치: 최대 5초를 넘지 않도록 제한
        return Math.min(threshold, 5000L);
    }
}