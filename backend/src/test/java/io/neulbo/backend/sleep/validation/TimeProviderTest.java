package io.neulbo.backend.sleep.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * TimeProvider 인터페이스와 DefaultTimeProvider 구현체의 기능 검증 테스트
 * 시간대 인식 시간 처리가 올바르게 동작하는지 확인
 */
class TimeProviderTest {

    private DefaultTimeProvider timeProvider;
    private Clock fixedClock;
    private ZoneId testZone;
    private Instant fixedInstant;

    @BeforeEach
    void setUp() {
        // 고정된 시간으로 테스트 (2024-01-15 12:00:00 UTC)
        fixedInstant = Instant.parse("2024-01-15T12:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        testZone = ZoneId.of("Asia/Seoul"); // UTC+9
        
        timeProvider = new DefaultTimeProvider(fixedClock, testZone);
    }

    @Test
    @DisplayName("기본 생성자가 UTC 기반으로 동작함")
    void defaultConstructor_UsesUtcBasedTime() {
        // Given
        DefaultTimeProvider defaultProvider = new DefaultTimeProvider();

        // When
        ZoneId applicationZone = defaultProvider.getApplicationZone();
        LocalDateTime now = defaultProvider.now();

        // Then
        assertThat(applicationZone).isEqualTo(ZoneOffset.UTC);
        assertThat(now).isNotNull();
        
        // 현재 시간과의 차이가 1초 이내인지 확인 (테스트 실행 시간 고려)
        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
        long secondsDiff = Math.abs(ChronoUnit.SECONDS.between(now, utcNow));
        assertThat(secondsDiff).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("커스텀 Clock과 시간대가 올바르게 설정됨")
    void customConstructor_UsesProvidedClockAndZone() {
        // When
        ZoneId applicationZone = timeProvider.getApplicationZone();
        Clock clock = timeProvider.getClock();

        // Then
        assertThat(applicationZone).isEqualTo(testZone);
        assertThat(clock).isEqualTo(fixedClock);
    }

    @Test
    @DisplayName("now() 메서드가 애플리케이션 시간대 기준 시간을 반환함")
    void now_ReturnsTimeInApplicationZone() {
        // When
        LocalDateTime now = timeProvider.now();

        // Then
        // 고정된 UTC 시간 (12:00)이 서울 시간대 (21:00)로 변환되어야 함
        LocalDateTime expected = LocalDateTime.of(2024, 1, 15, 21, 0, 0);
        assertThat(now).isEqualTo(expected);
    }

    @Test
    @DisplayName("nowInUtc() 메서드가 UTC 기준 시간을 반환함")
    void nowInUtc_ReturnsUtcTime() {
        // When
        ZonedDateTime utcTime = timeProvider.nowInUtc();

        // Then
        assertThat(utcTime.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(utcTime.toInstant()).isEqualTo(fixedInstant);
        
        LocalDateTime expectedLocal = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        assertThat(utcTime.toLocalDateTime()).isEqualTo(expectedLocal);
    }

    @Test
    @DisplayName("nowInZone() 메서드가 지정된 시간대의 시간을 반환함")
    void nowInZone_ReturnsTimeInSpecifiedZone() {
        // Given
        ZoneId newYorkZone = ZoneId.of("America/New_York"); // UTC-5 (EST) 또는 UTC-4 (EDT)

        // When
        ZonedDateTime newYorkTime = timeProvider.nowInZone(newYorkZone);

        // Then
        assertThat(newYorkTime.getZone()).isEqualTo(newYorkZone);
        assertThat(newYorkTime.toInstant()).isEqualTo(fixedInstant);
        
        // 1월 15일은 EST 기간이므로 UTC-5 (07:00)
        LocalDateTime expectedLocal = LocalDateTime.of(2024, 1, 15, 7, 0, 0);
        assertThat(newYorkTime.toLocalDateTime()).isEqualTo(expectedLocal);
    }

    @Test
    @DisplayName("nowInSystemZone() 메서드가 시스템 기본 시간대의 시간을 반환함")
    void nowInSystemZone_ReturnsSystemZoneTime() {
        // When
        ZonedDateTime systemTime = timeProvider.nowInSystemZone();

        // Then
        assertThat(systemTime.getZone()).isEqualTo(ZoneId.systemDefault());
        assertThat(systemTime.toInstant()).isEqualTo(fixedInstant);
    }

    @Test
    @DisplayName("nowInZoneAsLocalDateTime() 헬퍼 메서드가 올바르게 동작함")
    void nowInZoneAsLocalDateTime_ReturnsCorrectLocalDateTime() {
        // Given
        ZoneId parisZone = ZoneId.of("Europe/Paris"); // UTC+1 (CET) 또는 UTC+2 (CEST)

        // When
        LocalDateTime parisTime = timeProvider.nowInZoneAsLocalDateTime(parisZone);

        // Then
        // 1월 15일은 CET 기간이므로 UTC+1 (13:00)
        LocalDateTime expected = LocalDateTime.of(2024, 1, 15, 13, 0, 0);
        assertThat(parisTime).isEqualTo(expected);
    }

    @Test
    @DisplayName("nowInUtcAsLocalDateTime() 헬퍼 메서드가 올바르게 동작함")
    void nowInUtcAsLocalDateTime_ReturnsCorrectLocalDateTime() {
        // When
        LocalDateTime utcTime = timeProvider.nowInUtcAsLocalDateTime();

        // Then
        LocalDateTime expected = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        assertThat(utcTime).isEqualTo(expected);
    }

    @Test
    @DisplayName("getTimeInfo() 메서드가 디버깅 정보를 제공함")
    void getTimeInfo_ProvidesDebuggingInformation() {
        // When
        String timeInfo = timeProvider.getTimeInfo();

        // Then
        assertThat(timeInfo).contains("TimeProvider Info");
        assertThat(timeInfo).contains("UTC: 2024-01-15T12:00Z");
        assertThat(timeInfo).contains("Asia/Seoul");
        assertThat(timeInfo).contains("2024-01-15T21:00+09:00[Asia/Seoul]");
        assertThat(timeInfo).contains("System");
        
        System.out.println("TimeProvider Debug Info: " + timeInfo);
    }

    @Test
    @DisplayName("다양한 시간대에서 일관된 Instant 반환")
    void differentZones_ReturnConsistentInstant() {
        // Given
        ZoneId[] zones = {
            ZoneOffset.UTC,
            ZoneId.of("Asia/Seoul"),
            ZoneId.of("America/New_York"),
            ZoneId.of("Europe/London"),
            ZoneId.of("Australia/Sydney")
        };

        // When & Then
        for (ZoneId zone : zones) {
            ZonedDateTime zonedTime = timeProvider.nowInZone(zone);
            assertThat(zonedTime.toInstant()).isEqualTo(fixedInstant);
        }
    }

    @Test
    @DisplayName("실제 시간과 고정 시간의 동작 비교")
    void realTimeVsFixedTime_Comparison() {
        // Given
        DefaultTimeProvider realTimeProvider = new DefaultTimeProvider();
        
        // When
        LocalDateTime fixedTime = timeProvider.now();
        LocalDateTime realTime = realTimeProvider.now();
        
        // Then
        // 고정된 시간은 예측 가능해야 함
        LocalDateTime expectedFixed = LocalDateTime.of(2024, 1, 15, 21, 0, 0);
        assertThat(fixedTime).isEqualTo(expectedFixed);
        
        // 실제 시간은 현재 시간에 가까워야 함
        LocalDateTime currentUtc = LocalDateTime.now(ZoneOffset.UTC);
        long minutesDiff = Math.abs(ChronoUnit.MINUTES.between(realTime, currentUtc));
        assertThat(minutesDiff).isLessThanOrEqualTo(1);
        
        System.out.printf("Fixed time: %s, Real time: %s%n", fixedTime, realTime);
    }

    @Test
    @DisplayName("Clock 변경이 모든 시간 메서드에 영향을 줌")
    void clockChange_AffectsAllTimeMethods() {
        // Given
        Instant newInstant = Instant.parse("2024-06-15T18:30:00Z");
        Clock newClock = Clock.fixed(newInstant, ZoneOffset.UTC);
        DefaultTimeProvider newProvider = new DefaultTimeProvider(newClock, testZone);

        // When
        LocalDateTime appTime = newProvider.now();
        ZonedDateTime utcTime = newProvider.nowInUtc();
        ZonedDateTime zoneTime = newProvider.nowInZone(ZoneId.of("America/Los_Angeles"));

        // Then
        // 모든 시간이 새로운 Clock 기준으로 동작해야 함
        assertThat(utcTime.toInstant()).isEqualTo(newInstant);
        assertThat(zoneTime.toInstant()).isEqualTo(newInstant);
        
        // 서울 시간: UTC+9 → 06월 16일 03:30
        LocalDateTime expectedApp = LocalDateTime.of(2024, 6, 16, 3, 30, 0);
        assertThat(appTime).isEqualTo(expectedApp);
    }

    @Test
    @DisplayName("시간대별 일광절약시간(DST) 처리 확인")
    void daylightSavingTime_HandledCorrectly() {
        // Given - 일광절약시간이 적용되는 시기의 시간
        Instant summerInstant = Instant.parse("2024-07-15T12:00:00Z");
        Clock summerClock = Clock.fixed(summerInstant, ZoneOffset.UTC);
        DefaultTimeProvider summerProvider = new DefaultTimeProvider(summerClock, ZoneOffset.UTC);

        // When
        ZonedDateTime newYorkSummer = summerProvider.nowInZone(ZoneId.of("America/New_York"));
        ZonedDateTime londonSummer = summerProvider.nowInZone(ZoneId.of("Europe/London"));

        // Then
        // 7월은 EDT (UTC-4)와 BST (UTC+1) 적용
        assertThat(newYorkSummer.toLocalDateTime()).isEqualTo(LocalDateTime.of(2024, 7, 15, 8, 0, 0));
        assertThat(londonSummer.toLocalDateTime()).isEqualTo(LocalDateTime.of(2024, 7, 15, 13, 0, 0));
        
        System.out.printf("Summer DST - NYC: %s, London: %s%n", newYorkSummer, londonSummer);
    }

    @Test
    @DisplayName("TimeProvider 인터페이스 계약 준수 확인")
    void timeProviderInterface_ContractCompliance() {
        // TimeProvider 인터페이스로 참조하여 모든 메서드 동작 확인
        TimeProvider provider = timeProvider;

        // When & Then - 모든 메서드가 null을 반환하지 않아야 함
        assertThat(provider.now()).isNotNull();
        assertThat(provider.nowInUtc()).isNotNull();
        assertThat(provider.nowInSystemZone()).isNotNull();
        assertThat(provider.nowInZone(ZoneId.of("UTC"))).isNotNull();
        assertThat(provider.getApplicationZone()).isNotNull();
        assertThat(provider.getClock()).isNotNull();

        // 시간 일관성 확인
        Instant baseInstant = provider.getClock().instant();
        assertThat(provider.nowInUtc().toInstant()).isEqualTo(baseInstant);
        assertThat(provider.nowInZone(ZoneOffset.UTC).toInstant()).isEqualTo(baseInstant);
    }
}