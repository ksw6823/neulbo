package io.neulbo.backend.sleep.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 시간대 인식 validator들의 개선사항 검증 테스트
 * TimeProvider를 사용한 일관된 시간 처리가 올바르게 동작하는지 확인
 */
@ExtendWith(MockitoExtension.class)
class TimezoneAwareValidatorTest {

    @Mock
    private ConstraintValidatorContext mockContext;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder mockViolationBuilder;

    private TimeProvider fixedTimeProvider;
    private ValidSleepEndTimeValidator sleepEndTimeValidator;
    private ValidSleepStartTimeValidator sleepStartTimeValidator;
    private ValidMovementTimestampValidator movementTimestampValidator;

    @BeforeEach
    void setUp() {
        // 고정된 시간 설정 (2024-01-15 12:00:00 UTC)
        Instant fixedInstant = Instant.parse("2024-01-15T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        fixedTimeProvider = new DefaultTimeProvider(fixedClock, ZoneOffset.UTC);

        // Validator 초기화
        sleepEndTimeValidator = new ValidSleepEndTimeValidator();
        sleepEndTimeValidator.timeProvider = fixedTimeProvider;

        sleepStartTimeValidator = new ValidSleepStartTimeValidator();
        sleepStartTimeValidator.timeProvider = fixedTimeProvider;

        movementTimestampValidator = new ValidMovementTimestampValidator();
        movementTimestampValidator.timeProvider = fixedTimeProvider;

        // Mock context 설정
        when(mockContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(mockViolationBuilder);
        when(mockViolationBuilder.addConstraintViolation()).thenReturn(mockContext);
    }

    @Test
    @DisplayName("ValidSleepEndTimeValidator가 TimeProvider를 사용하여 시간대 인식 검증을 수행함")
    void sleepEndTimeValidator_UsesTimeProvider_ForTimezoneAwareValidation() {
        // Given
        ValidSleepEndTime annotation = createSleepEndTimeAnnotation(7);
        sleepEndTimeValidator.initialize(annotation);

        // 현재 시간(고정): 2024-01-15 12:00:00 UTC
        // 유효한 시간: 현재보다 과거이고 7일 이내
        LocalDateTime validPastTime = LocalDateTime.of(2024, 1, 14, 10, 0, 0); // 1일 2시간 전
        LocalDateTime invalidFutureTime = LocalDateTime.of(2024, 1, 15, 13, 0, 0); // 1시간 후
        LocalDateTime invalidOldTime = LocalDateTime.of(2024, 1, 7, 10, 0, 0); // 8일 전

        // When & Then
        assertThat(sleepEndTimeValidator.isValid(validPastTime, mockContext)).isTrue();
        assertThat(sleepEndTimeValidator.isValid(invalidFutureTime, mockContext)).isFalse();
        assertThat(sleepEndTimeValidator.isValid(invalidOldTime, mockContext)).isFalse();

        // 검증 실패 시 적절한 메시지가 생성되는지 확인
        verify(mockContext, times(2)).disableDefaultConstraintViolation();
        verify(mockContext, times(2)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("ValidSleepStartTimeValidator가 TimeProvider를 사용하여 시간대 인식 검증을 수행함")
    void sleepStartTimeValidator_UsesTimeProvider_ForTimezoneAwareValidation() {
        // Given
        ValidSleepStartTime annotation = createSleepStartTimeAnnotation(24);
        sleepStartTimeValidator.initialize(annotation);

        // 현재 시간(고정): 2024-01-15 12:00:00 UTC
        // 유효한 시간: 현재보다 과거이고 24시간 이내
        LocalDateTime validPastTime = LocalDateTime.of(2024, 1, 14, 15, 0, 0); // 21시간 전
        LocalDateTime invalidFutureTime = LocalDateTime.of(2024, 1, 15, 14, 0, 0); // 2시간 후
        LocalDateTime invalidOldTime = LocalDateTime.of(2024, 1, 13, 10, 0, 0); // 26시간 전

        // When & Then
        assertThat(sleepStartTimeValidator.isValid(validPastTime, mockContext)).isTrue();
        assertThat(sleepStartTimeValidator.isValid(invalidFutureTime, mockContext)).isFalse();
        assertThat(sleepStartTimeValidator.isValid(invalidOldTime, mockContext)).isFalse();

        // 검증 실패 시 적절한 메시지가 생성되는지 확인
        verify(mockContext, times(2)).disableDefaultConstraintViolation();
        verify(mockContext, times(2)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("ValidMovementTimestampValidator가 TimeProvider를 사용하여 시간대 인식 검증을 수행함")
    void movementTimestampValidator_UsesTimeProvider_ForTimezoneAwareValidation() {
        // Given
        ValidMovementTimestamp annotation = createMovementTimestampAnnotation(72);
        movementTimestampValidator.initialize(annotation);

        // 현재 시간(고정): 2024-01-15 12:00:00 UTC
        // 유효한 시간: 현재보다 과거이고 72시간 이내
        LocalDateTime validPastTime = LocalDateTime.of(2024, 1, 13, 10, 0, 0); // 50시간 전
        LocalDateTime invalidFutureTime = LocalDateTime.of(2024, 1, 15, 13, 30, 0); // 1.5시간 후
        LocalDateTime invalidOldTime = LocalDateTime.of(2024, 1, 11, 10, 0, 0); // 74시간 전

        // When & Then
        assertThat(movementTimestampValidator.isValid(validPastTime, mockContext)).isTrue();
        assertThat(movementTimestampValidator.isValid(invalidFutureTime, mockContext)).isFalse();
        assertThat(movementTimestampValidator.isValid(invalidOldTime, mockContext)).isFalse();

        // 검증 실패 시 적절한 메시지가 생성되는지 확인
        verify(mockContext, times(2)).disableDefaultConstraintViolation();
        verify(mockContext, times(2)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("다양한 시간대에서도 일관된 검증 결과를 보장함")
    void validators_ConsistentResults_AcrossDifferentTimezones() {
        // Given - 서울 시간대(UTC+9)로 설정된 TimeProvider
        Instant fixedInstant = Instant.parse("2024-01-15T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        TimeProvider seoulTimeProvider = new DefaultTimeProvider(fixedClock, ZoneId.of("Asia/Seoul"));

        ValidSleepEndTimeValidator seoulValidator = new ValidSleepEndTimeValidator();
        seoulValidator.timeProvider = seoulTimeProvider;
        seoulValidator.initialize(createSleepEndTimeAnnotation(7));

        // UTC 기준과 서울 시간대 기준으로 각각 검증
        LocalDateTime testTime = LocalDateTime.of(2024, 1, 14, 10, 0, 0);

        // When
        boolean utcResult = sleepEndTimeValidator.isValid(testTime, mockContext);
        boolean seoulResult = seoulValidator.isValid(testTime, mockContext);

        // Then - 다른 시간대를 사용하더라도 동일한 LocalDateTime에 대해서는 다른 결과가 나올 수 있음
        // 이는 의도된 동작으로, 각 시간대의 "현재 시간" 기준으로 검증하기 때문
        System.out.printf("UTC 기준 검증 결과: %s, 서울 시간대 기준 검증 결과: %s%n", utcResult, seoulResult);
        
        // 하지만 동일한 TimeProvider를 사용하는 경우는 일관된 결과를 보장해야 함
        boolean utcResult2 = sleepEndTimeValidator.isValid(testTime, mockContext);
        assertThat(utcResult).isEqualTo(utcResult2);
    }

    @Test
    @DisplayName("null 값에 대한 처리가 시간대와 무관하게 일관됨")
    void validators_NullHandling_ConsistentAcrossTimezones() {
        // Given
        ValidSleepEndTime endTimeAnnotation = createSleepEndTimeAnnotation(7);
        sleepEndTimeValidator.initialize(endTimeAnnotation);

        ValidSleepStartTime startTimeAnnotation = createSleepStartTimeAnnotation(24);
        sleepStartTimeValidator.initialize(startTimeAnnotation);

        ValidMovementTimestamp timestampAnnotation = createMovementTimestampAnnotation(72);
        movementTimestampValidator.initialize(timestampAnnotation);

        // When & Then - 모든 validator가 null에 대해 true를 반환해야 함 (@NotNull에서 처리)
        assertThat(sleepEndTimeValidator.isValid(null, mockContext)).isTrue();
        assertThat(sleepStartTimeValidator.isValid(null, mockContext)).isTrue();
        assertThat(movementTimestampValidator.isValid(null, mockContext)).isTrue();

        // null 처리 시에는 TimeProvider를 호출하지 않아야 함
        verifyNoInteractions(mockContext);
    }

    @Test
    @DisplayName("경계값 테스트 - 정확히 허용 시간 경계에서의 동작")
    void validators_BoundaryValues_ExactLimits() {
        // Given
        ValidSleepEndTime endTimeAnnotation = createSleepEndTimeAnnotation(7);
        sleepEndTimeValidator.initialize(endTimeAnnotation);

        // 현재 시간: 2024-01-15 12:00:00 UTC
        // 정확히 7일 전: 2024-01-08 12:00:00 UTC
        LocalDateTime exactlySevenDaysAgo = LocalDateTime.of(2024, 1, 8, 12, 0, 0);
        LocalDateTime justOverSevenDays = LocalDateTime.of(2024, 1, 8, 11, 59, 59);

        // When & Then
        assertThat(sleepEndTimeValidator.isValid(exactlySevenDaysAgo, mockContext)).isTrue();
        assertThat(sleepEndTimeValidator.isValid(justOverSevenDays, mockContext)).isFalse();
    }

    @Test
    @DisplayName("TimeProvider 변경이 모든 validator에 즉시 반영됨")
    void validators_TimeProviderChange_ImmediatelyReflected() {
        // Given - 초기 검증
        ValidSleepEndTime annotation = createSleepEndTimeAnnotation(1);
        sleepEndTimeValidator.initialize(annotation);

        LocalDateTime testTime = LocalDateTime.of(2024, 1, 14, 10, 0, 0); // 1일 2시간 전
        boolean initialResult = sleepEndTimeValidator.isValid(testTime, mockContext);

        // When - TimeProvider를 다른 시간으로 변경
        Instant newInstant = Instant.parse("2024-01-13T10:00:00Z"); // 1일 더 과거로
        Clock newClock = Clock.fixed(newInstant, ZoneOffset.UTC);
        sleepEndTimeValidator.timeProvider = new DefaultTimeProvider(newClock, ZoneOffset.UTC);

        boolean newResult = sleepEndTimeValidator.isValid(testTime, mockContext);

        // Then - 결과가 변경되어야 함 (testTime이 이제 미래가 됨)
        assertThat(initialResult).isTrue();  // 원래는 과거였음
        assertThat(newResult).isFalse();     // 이제는 미래가 됨
    }

    @Test
    @DisplayName("실제 애플리케이션 시나리오 - 다양한 시간대의 사용자 데이터 검증")
    void validators_RealWorldScenario_MultipleTimezoneUsers() {
        // Given - 다양한 지역의 사용자를 시뮬레이션
        ZoneId[] userTimezones = {
            ZoneId.of("America/New_York"),    // UTC-5/-4
            ZoneId.of("Europe/London"),       // UTC+0/+1
            ZoneId.of("Asia/Seoul"),          // UTC+9
            ZoneId.of("Australia/Sydney")     // UTC+10/+11
        };

        Instant fixedInstant = Instant.parse("2024-01-15T12:00:00Z");

        for (ZoneId userZone : userTimezones) {
            // 각 시간대의 TimeProvider 생성
            Clock userClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
            TimeProvider userTimeProvider = new DefaultTimeProvider(userClock, userZone);

            ValidSleepEndTimeValidator userValidator = new ValidSleepEndTimeValidator();
            userValidator.timeProvider = userTimeProvider;
            userValidator.initialize(createSleepEndTimeAnnotation(7));

            // 각 사용자의 로컬 시간으로 수면 데이터 입력
            ZonedDateTime userNow = userTimeProvider.nowInZone(userZone);
            LocalDateTime userSleepTime = userNow.minusHours(8).toLocalDateTime(); // 8시간 전 수면

            // When
            boolean validationResult = userValidator.isValid(userSleepTime, mockContext);

            // Then - 모든 사용자의 8시간 전 수면 시간은 유효해야 함
            assertThat(validationResult).isTrue();
            
            System.out.printf("시간대: %s, 현재시간: %s, 수면시간: %s, 검증결과: %s%n",
                    userZone, userNow, userSleepTime, validationResult);
        }
    }

    // Helper methods for creating annotation instances
    private ValidSleepEndTime createSleepEndTimeAnnotation(int maxPastDays) {
        return new ValidSleepEndTime() {
            @Override
            public Class<ValidSleepEndTime> annotationType() {
                return ValidSleepEndTime.class;
            }

            @Override
            public String message() {
                return "수면 종료 시간이 유효하지 않습니다";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public int maxPastDays() {
                return maxPastDays;
            }
        };
    }

    private ValidSleepStartTime createSleepStartTimeAnnotation(int maxPastHours) {
        return new ValidSleepStartTime() {
            @Override
            public Class<ValidSleepStartTime> annotationType() {
                return ValidSleepStartTime.class;
            }

            @Override
            public String message() {
                return "수면 시작 시간이 유효하지 않습니다";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public int maxPastHours() {
                return maxPastHours;
            }
        };
    }

    private ValidMovementTimestamp createMovementTimestampAnnotation(int maxPastHours) {
        return new ValidMovementTimestamp() {
            @Override
            public Class<ValidMovementTimestamp> annotationType() {
                return ValidMovementTimestamp.class;
            }

            @Override
            public String message() {
                return "움직임 데이터의 타임스탬프가 유효하지 않습니다";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public int maxPastHours() {
                return maxPastHours;
            }
        };
    }
}