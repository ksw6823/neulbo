package io.neulbo.backend.sleep.service;

import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.exception.BusinessException;
import io.neulbo.backend.sleep.domain.SleepSession;
import io.neulbo.backend.sleep.domain.SessionStatus;
import io.neulbo.backend.sleep.repository.MovementDataRepository;
import io.neulbo.backend.sleep.repository.SleepSessionRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MovementDataService의 타입 안전성 개선을 검증하는 테스트
 */
@ExtendWith(MockitoExtension.class)
class MovementDataServiceTypeSafetyTest {

    @Mock
    private MovementDataRepository movementDataRepository;

    @Mock
    private SleepSessionRepository sleepSessionRepository;

    @InjectMocks
    private MovementDataService movementDataService;

    private User testUser;
    private SleepSession testSession;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .provider("google")
                .providerId("test123")
                .username("testuser")
                .build();

        // SleepSession은 builder가 없으므로 리플렉션으로 생성하거나 실제 생성자 사용
        testSession = createTestSleepSession();

        testTime = LocalDateTime.now().minusHours(4);
    }

    /**
     * 테스트용 SleepSession 생성 (builder가 없으므로 Mock 사용)
     */
    private SleepSession createTestSleepSession() {
        SleepSession session = mock(SleepSession.class);
        when(session.getId()).thenReturn(1L);
        when(session.getUser()).thenReturn(testUser);
        when(session.getSessionStatus()).thenReturn(SessionStatus.COMPLETED);
        when(session.getSleepStartTime()).thenReturn(LocalDateTime.now().minusHours(8));
        return session;
    }

    @Test
    @DisplayName("정상적인 Object[] 데이터로 시간대별 움직임 패턴 분석 성공")
    void analyzeHourlyMovementPattern_Success() {
        // Given
        Long sessionId = 1L;
        List<Object[]> mockHourlyStats = List.of(
            new Object[]{testTime, 10L, 5.5},
            new Object[]{testTime.plusHours(1), 15L, 7.2},
            new Object[]{testTime.plusHours(2), 8L, 3.1}
        );

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(movementDataRepository.findHourlyMovementStatsBySleepSession(testSession))
                .thenReturn(mockHourlyStats);

        // When
        List<MovementDataService.HourlyMovementPattern> result = 
                movementDataService.analyzeHourlyMovementPattern(testUser, sessionId);

        // Then
        assertThat(result).hasSize(3);
        
        assertThat(result.get(0).getHour()).isEqualTo(testTime);
        assertThat(result.get(0).getMovementCount()).isEqualTo(10L);
        assertThat(result.get(0).getAverageIntensity()).isEqualTo(5.5);
        
        assertThat(result.get(1).getHour()).isEqualTo(testTime.plusHours(1));
        assertThat(result.get(1).getMovementCount()).isEqualTo(15L);
        assertThat(result.get(1).getAverageIntensity()).isEqualTo(7.2);
        
        assertThat(result.get(2).getHour()).isEqualTo(testTime.plusHours(2));
        assertThat(result.get(2).getMovementCount()).isEqualTo(8L);
        assertThat(result.get(2).getAverageIntensity()).isEqualTo(3.1);
    }

    @Test
    @DisplayName("Integer 타입의 Number도 안전하게 Long으로 변환")
    void analyzeHourlyMovementPattern_IntegerToLong_Success() {
        // Given
        Long sessionId = 1L;
        List<Object[]> mockHourlyStats = Arrays.<Object[]>asList(
            new Object[]{testTime, Integer.valueOf(10), Double.valueOf(5.5)}
        );

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(movementDataRepository.findHourlyMovementStatsBySleepSession(testSession))
                .thenReturn(mockHourlyStats);

        // When
        List<MovementDataService.HourlyMovementPattern> result = 
                movementDataService.analyzeHourlyMovementPattern(testUser, sessionId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMovementCount()).isEqualTo(10L);
        assertThat(result.get(0).getAverageIntensity()).isEqualTo(5.5);
    }

    @Test
    @DisplayName("Float 타입의 Number도 안전하게 Double로 변환")
    void analyzeHourlyMovementPattern_FloatToDouble_Success() {
        // Given
        Long sessionId = 1L;
        List<Object[]> mockHourlyStats = Arrays.<Object[]>asList(
            new Object[]{testTime, 10L, Float.valueOf(5.5f)}
        );

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(movementDataRepository.findHourlyMovementStatsBySleepSession(testSession))
                .thenReturn(mockHourlyStats);

        // When
        List<MovementDataService.HourlyMovementPattern> result = 
                movementDataService.analyzeHourlyMovementPattern(testUser, sessionId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAverageIntensity()).isEqualTo(5.5, within(0.01));
    }

    @Test
    @DisplayName("null이 포함된 Number 필드는 기본값으로 처리")
    void analyzeHourlyMovementPattern_NullNumberFields_UseDefaults() {
        // Given
        Long sessionId = 1L;
        List<Object[]> mockHourlyStats = Arrays.<Object[]>asList(
            new Object[]{testTime, null, null}  // movementCount와 avgIntensity가 null
        );

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(movementDataRepository.findHourlyMovementStatsBySleepSession(testSession))
                .thenReturn(mockHourlyStats);

        // When
        List<MovementDataService.HourlyMovementPattern> result = 
                movementDataService.analyzeHourlyMovementPattern(testUser, sessionId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHour()).isEqualTo(testTime);
        assertThat(result.get(0).getMovementCount()).isEqualTo(0L);  // 기본값
        assertThat(result.get(0).getAverageIntensity()).isEqualTo(0.0);  // 기본값
    }

    @Test
    @DisplayName("잘못된 타입의 LocalDateTime 필드로 ClassCastException 발생")
    void analyzeHourlyMovementPattern_InvalidLocalDateTimeType_ThrowsException() {
        // Given
        Long sessionId = 1L;
        List<Object[]> mockHourlyStats = Arrays.<Object[]>asList(
            new Object[]{"2023-01-01", 10L, 5.5}  // String은 LocalDateTime이 아님
        );

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(movementDataRepository.findHourlyMovementStatsBySleepSession(testSession))
                .thenReturn(mockHourlyStats);

        // When & Then
        assertThatThrownBy(() -> movementDataService.analyzeHourlyMovementPattern(testUser, sessionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SLEEP_ANALYSIS_FAILED);
    }

    @Test
    @DisplayName("잘못된 타입의 Number 필드로 ClassCastException 발생")
    void analyzeHourlyMovementPattern_InvalidNumberType_ThrowsException() {
        // Given
        Long sessionId = 1L;
        List<Object[]> mockHourlyStats = Arrays.<Object[]>asList(
            new Object[]{testTime, "10", 5.5}  // String은 Number가 아님
        );

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(movementDataRepository.findHourlyMovementStatsBySleepSession(testSession))
                .thenReturn(mockHourlyStats);

        // When & Then
        assertThatThrownBy(() -> movementDataService.analyzeHourlyMovementPattern(testUser, sessionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SLEEP_ANALYSIS_FAILED);
    }

    @Test
    @DisplayName("배열 크기가 부족할 때 ArrayIndexOutOfBoundsException 발생")
    void analyzeHourlyMovementPattern_InsufficientArraySize_ThrowsException() {
        // Given
        Long sessionId = 1L;
        List<Object[]> mockHourlyStats = Arrays.<Object[]>asList(
            new Object[]{testTime, 10L}  // avgIntensity 필드 누락 (index 2)
        );

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(movementDataRepository.findHourlyMovementStatsBySleepSession(testSession))
                .thenReturn(mockHourlyStats);

        // When & Then
        assertThatThrownBy(() -> movementDataService.analyzeHourlyMovementPattern(testUser, sessionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SLEEP_ANALYSIS_FAILED);
    }

    @Test
    @DisplayName("null LocalDateTime 필드로 IllegalArgumentException 발생")
    void analyzeHourlyMovementPattern_NullLocalDateTime_ThrowsException() {
        // Given
        Long sessionId = 1L;
        List<Object[]> mockHourlyStats = Arrays.<Object[]>asList(
            new Object[]{null, 10L, 5.5}  // hour가 null
        );

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(movementDataRepository.findHourlyMovementStatsBySleepSession(testSession))
                .thenReturn(mockHourlyStats);

        // When & Then
        assertThatThrownBy(() -> movementDataService.analyzeHourlyMovementPattern(testUser, sessionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SLEEP_ANALYSIS_FAILED);
    }

    @Test
    @DisplayName("빈 배열도 안전하게 처리")
    void analyzeHourlyMovementPattern_EmptyArray_Success() {
        // Given
        Long sessionId = 1L;
        List<Object[]> mockHourlyStats = Arrays.asList();

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(movementDataRepository.findHourlyMovementStatsBySleepSession(testSession))
                .thenReturn(mockHourlyStats);

        // When
        List<MovementDataService.HourlyMovementPattern> result = 
                movementDataService.analyzeHourlyMovementPattern(testUser, sessionId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("접근 권한이 없는 세션으로 ACCESS_DENIED 예외 발생")
    void analyzeHourlyMovementPattern_AccessDenied_ThrowsException() {
        // Given
        Long sessionId = 1L;
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .provider("google")
                .providerId("other123")
                .username("otheruser")
                .build();

        SleepSession otherUserSession = mock(SleepSession.class);
        when(otherUserSession.getId()).thenReturn(1L);
        when(otherUserSession.getUser()).thenReturn(otherUser);
        when(otherUserSession.getSessionStatus()).thenReturn(SessionStatus.COMPLETED);

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(otherUserSession));

        // When & Then
        assertThatThrownBy(() -> movementDataService.analyzeHourlyMovementPattern(testUser, sessionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("존재하지 않는 세션으로 SLEEP_SESSION_NOT_FOUND 예외 발생")
    void analyzeHourlyMovementPattern_SessionNotFound_ThrowsException() {
        // Given
        Long sessionId = 999L;

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> movementDataService.analyzeHourlyMovementPattern(testUser, sessionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SLEEP_SESSION_NOT_FOUND);
    }

    @Test
    @DisplayName("다양한 Number 타입 조합 테스트")
    void analyzeHourlyMovementPattern_VariousNumberTypes_Success() {
        // Given
        Long sessionId = 1L;
        List<Object[]> mockHourlyStats = Arrays.asList(
            new Object[]{testTime, Byte.valueOf((byte) 5), Float.valueOf(1.1f)},
            new Object[]{testTime.plusHours(1), Short.valueOf((short) 15), Double.valueOf(2.2)},
            new Object[]{testTime.plusHours(2), Integer.valueOf(25), Long.valueOf(3L).doubleValue()},
            new Object[]{testTime.plusHours(3), Long.valueOf(35), Integer.valueOf(4).doubleValue()}
        );

        when(sleepSessionRepository.findById(sessionId)).thenReturn(Optional.of(testSession));
        when(movementDataRepository.findHourlyMovementStatsBySleepSession(testSession))
                .thenReturn(mockHourlyStats);

        // When
        List<MovementDataService.HourlyMovementPattern> result = 
                movementDataService.analyzeHourlyMovementPattern(testUser, sessionId);

        // Then
        assertThat(result).hasSize(4);
        
        // Byte to Long, Float to Double
        assertThat(result.get(0).getMovementCount()).isEqualTo(5L);
        assertThat(result.get(0).getAverageIntensity()).isEqualTo(1.1, within(0.01));
        
        // Short to Long, Double to Double
        assertThat(result.get(1).getMovementCount()).isEqualTo(15L);
        assertThat(result.get(1).getAverageIntensity()).isEqualTo(2.2);
        
        // Integer to Long, Double to Double
        assertThat(result.get(2).getMovementCount()).isEqualTo(25L);
        assertThat(result.get(2).getAverageIntensity()).isEqualTo(4.0);
        
        // Long to Long, Double to Double
        assertThat(result.get(3).getMovementCount()).isEqualTo(35L);
        assertThat(result.get(3).getAverageIntensity()).isEqualTo(4.0);
    }
}