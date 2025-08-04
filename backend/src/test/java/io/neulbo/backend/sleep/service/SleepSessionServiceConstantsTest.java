package io.neulbo.backend.sleep.service;

import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SessionStatus;
import io.neulbo.backend.sleep.repository.SleepSessionRepository;
import io.neulbo.backend.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SleepSessionService의 하드코딩된 시간 값들이 상수로 올바르게 추출되었는지 검증하는 테스트
 */
@ExtendWith(MockitoExtension.class)
class SleepSessionServiceConstantsTest {

    @Mock
    private SleepSessionRepository sleepSessionRepository;

    @InjectMocks
    private SleepSessionService sleepSessionService;

    private User testUser;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.now().minusHours(25); // 25시간 전

        testUser = User.builder()
                .id(UUID.randomUUID())
                .provider("google")
                .providerId("test123")
                .username("testuser")
                .build();
    }

    private SleepSession createMockSleepSession(Long id, SessionStatus status, LocalDateTime createdAt) {
        SleepSession session = mock(SleepSession.class);
        when(session.getId()).thenReturn(id);
        when(session.getUser()).thenReturn(testUser);
        when(session.getSessionStatus()).thenReturn(status);
        when(session.getCreatedAt()).thenReturn(createdAt);
        return session;
    }

    @Test
    @DisplayName("STALE_SESSION_HOURS 상수가 올바른 값(24)으로 정의됨")
    void staleSessionHours_ConstantHasCorrectValue() throws Exception {
        // Given & When - Reflection을 사용하여 private static final 상수 접근
        Field staleSessionHoursField = SleepSessionService.class.getDeclaredField("STALE_SESSION_HOURS");
        staleSessionHoursField.setAccessible(true);
        int staleSessionHours = (int) staleSessionHoursField.get(null); // static field이므로 null 전달

        // Then
        assertThat(staleSessionHours).isEqualTo(24);
    }

    @Test
    @DisplayName("ASSUMED_SLEEP_HOURS 상수가 올바른 값(8)으로 정의됨")
    void assumedSleepHours_ConstantHasCorrectValue() throws Exception {
        // Given & When - Reflection을 사용하여 private static final 상수 접근
        Field assumedSleepHoursField = SleepSessionService.class.getDeclaredField("ASSUMED_SLEEP_HOURS");
        assumedSleepHoursField.setAccessible(true);
        int assumedSleepHours = (int) assumedSleepHoursField.get(null); // static field이므로 null 전달

        // Then
        assertThat(assumedSleepHours).isEqualTo(8);
    }

    @Test
    @DisplayName("cleanupStaleSessions 메서드가 상수를 사용하여 올바른 cutoff time을 계산함")
    void cleanupStaleSessions_UsesConstantForCutoffTime() {
        // Given
        SleepSession staleSession = createMockSleepSession(1L, SessionStatus.IN_PROGRESS, baseTime);
        when(sleepSessionRepository.findStaleInProgressSessions(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(staleSession));
        when(sleepSessionRepository.saveAll(any())).thenReturn(Collections.emptyList());

        // When
        sleepSessionService.cleanupStaleSessions();

        // Then
        // STALE_SESSION_HOURS(24시간) 전의 시간으로 cutoff time이 계산되는지 확인
        verify(sleepSessionRepository).findStaleInProgressSessions(argThat(cutoffTime -> {
            LocalDateTime expected = LocalDateTime.now().minusHours(24);
            // 테스트 실행 시간 차이를 고려하여 1분 오차 허용
            return Math.abs(java.time.Duration.between(cutoffTime, expected).toMinutes()) <= 1;
        }));
    }

    @Test
    @DisplayName("cleanupStaleSessions 메서드가 상수를 사용하여 올바른 수면 종료 시간을 설정함")
    void cleanupStaleSessions_UsesConstantForSleepEndTime() {
        // Given
        SleepSession staleSession = createMockSleepSession(1L, SessionStatus.IN_PROGRESS, baseTime);
        when(sleepSessionRepository.findStaleInProgressSessions(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(staleSession));
        when(sleepSessionRepository.saveAll(any())).thenReturn(Collections.emptyList());

        // When
        sleepSessionService.cleanupStaleSessions();

        // Then
        // ASSUMED_SLEEP_HOURS(8시간) 후의 시간으로 수면 종료 시간이 설정되는지 확인
        verify(staleSession).endSleep(argThat(endTime -> {
            LocalDateTime expected = baseTime.plusHours(8);
            return endTime.equals(expected);
        }));
    }

    @Test
    @DisplayName("상수 사용으로 유지보수성이 개선됨 - 값 변경 시 한 곳에서만 수정")
    void constants_ImproveMaintenability() throws Exception {
        // Given - 상수들 확인
        Field staleSessionHoursField = SleepSessionService.class.getDeclaredField("STALE_SESSION_HOURS");
        Field assumedSleepHoursField = SleepSessionService.class.getDeclaredField("ASSUMED_SLEEP_HOURS");
        
        staleSessionHoursField.setAccessible(true);
        assumedSleepHoursField.setAccessible(true);

        // When
        int staleSessionHours = (int) staleSessionHoursField.get(null);
        int assumedSleepHours = (int) assumedSleepHoursField.get(null);

        // Then - 상수들이 static final로 정의되어 있는지 확인
        assertThat(java.lang.reflect.Modifier.isStatic(staleSessionHoursField.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isFinal(staleSessionHoursField.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isPrivate(staleSessionHoursField.getModifiers())).isTrue();

        assertThat(java.lang.reflect.Modifier.isStatic(assumedSleepHoursField.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isFinal(assumedSleepHoursField.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isPrivate(assumedSleepHoursField.getModifiers())).isTrue();

        // 의미있는 값들인지 확인
        assertThat(staleSessionHours).isPositive().isLessThanOrEqualTo(48); // 최대 2일
        assertThat(assumedSleepHours).isPositive().isLessThanOrEqualTo(12); // 최대 12시간 수면
        
        System.out.printf("상수 정의 확인: STALE_SESSION_HOURS=%d, ASSUMED_SLEEP_HOURS=%d%n", 
                staleSessionHours, assumedSleepHours);
    }

    @Test
    @DisplayName("중단된 세션 정리 시 상수 값들이 실제 비즈니스 로직에 올바르게 적용됨")
    void cleanupStaleSessions_ConstantsAppliedCorrectlyInBusinessLogic() {
        // Given
        LocalDateTime sessionCreatedTime = LocalDateTime.now().minusHours(25); // 25시간 전 생성된 세션
        SleepSession staleSession = createMockSleepSession(1L, SessionStatus.IN_PROGRESS, sessionCreatedTime);
        
        when(sleepSessionRepository.findStaleInProgressSessions(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(staleSession));
        when(sleepSessionRepository.saveAll(any())).thenReturn(Collections.emptyList());

        // When
        sleepSessionService.cleanupStaleSessions();

        // Then
        // 1. 24시간 이상 된 세션들을 찾는 쿼리가 호출됨
        verify(sleepSessionRepository).findStaleInProgressSessions(argThat(cutoffTime -> {
            // 현재 시간에서 24시간을 뺀 시간과 비교
            long hoursDiff = java.time.Duration.between(cutoffTime, LocalDateTime.now()).toHours();
            return Math.abs(hoursDiff - 24) <= 1; // 1시간 오차 허용
        }));

        // 2. 8시간으로 가정하여 수면 종료 처리
        LocalDateTime expectedEndTime = sessionCreatedTime.plusHours(8);
        verify(staleSession).endSleep(expectedEndTime);

        // 3. 세션들이 저장됨
        verify(sleepSessionRepository).saveAll(Arrays.asList(staleSession));
    }

    @Test
    @DisplayName("빈 중단된 세션 목록에 대해서도 정상 처리됨")
    void cleanupStaleSessions_HandlesEmptyStaleSessionsList() {
        // Given
        when(sleepSessionRepository.findStaleInProgressSessions(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        sleepSessionService.cleanupStaleSessions();

        // Then
        // cutoff time 계산을 위한 호출은 발생
        verify(sleepSessionRepository).findStaleInProgressSessions(any(LocalDateTime.class));
        
        // 빈 목록이므로 saveAll은 호출되지 않음
        verify(sleepSessionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("다양한 시나리오에서 상수 값들이 일관되게 적용됨")
    void constants_AppliedConsistentlyAcrossScenarios() {
        // Given - 여러 개의 중단된 세션들
        LocalDateTime session1Created = LocalDateTime.now().minusHours(25);
        LocalDateTime session2Created = LocalDateTime.now().minusHours(30);
        LocalDateTime session3Created = LocalDateTime.now().minusHours(48);

        SleepSession session1 = createMockSleepSession(1L, SessionStatus.IN_PROGRESS, session1Created);
        SleepSession session2 = createMockSleepSession(2L, SessionStatus.IN_PROGRESS, session2Created);
        SleepSession session3 = createMockSleepSession(3L, SessionStatus.IN_PROGRESS, session3Created);

        when(sleepSessionRepository.findStaleInProgressSessions(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(session1, session2, session3));
        when(sleepSessionRepository.saveAll(any())).thenReturn(Collections.emptyList());

        // When
        sleepSessionService.cleanupStaleSessions();

        // Then - 모든 세션에 동일한 8시간 가정이 적용됨
        verify(session1).endSleep(session1Created.plusHours(8));
        verify(session2).endSleep(session2Created.plusHours(8));
        verify(session3).endSleep(session3Created.plusHours(8));

        // 한 번의 배치 저장 호출
        verify(sleepSessionRepository).saveAll(Arrays.asList(session1, session2, session3));
    }

    @Test
    @DisplayName("상수 추출로 코드 가독성과 의미 전달이 개선됨")
    void constants_ImproveCodeReadabilityAndMeaning() throws Exception {
        // 이 테스트는 리팩토링의 효과를 문서화하는 목적

        // Given - 상수들의 이름과 값 확인
        Field staleField = SleepSessionService.class.getDeclaredField("STALE_SESSION_HOURS");
        Field assumedField = SleepSessionService.class.getDeclaredField("ASSUMED_SLEEP_HOURS");
        
        staleField.setAccessible(true);
        assumedField.setAccessible(true);

        // When
        String staleName = staleField.getName();
        String assumedName = assumedField.getName();
        int staleValue = (int) staleField.get(null);
        int assumedValue = (int) assumedField.get(null);

        // Then - 상수 이름이 의미를 명확히 전달하는지 확인
        assertThat(staleName).contains("STALE").contains("SESSION").contains("HOURS");
        assertThat(assumedName).contains("ASSUMED").contains("SLEEP").contains("HOURS");

        // 값들이 비즈니스 로직에 맞는지 확인
        assertThat(staleValue).as("중단된 세션 판단 기준 시간").isEqualTo(24);
        assertThat(assumedValue).as("가정된 수면 시간").isEqualTo(8);

        System.out.printf("리팩토링 결과:%n");
        System.out.printf("- 하드코딩된 '24' → %s = %d%n", staleName, staleValue);
        System.out.printf("- 하드코딩된 '8' → %s = %d%n", assumedName, assumedValue);
        System.out.printf("→ 코드의 의미가 명확해지고 유지보수성이 향상됨%n");
    }
}