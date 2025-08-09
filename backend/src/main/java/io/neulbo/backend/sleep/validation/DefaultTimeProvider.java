package io.neulbo.backend.sleep.validation;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * TimeProvider의 기본 구현체
 * 애플리케이션 전반에서 일관된 시간대 처리를 제공
 */
@Component
public class DefaultTimeProvider implements TimeProvider {

    private final Clock clock;
    private final ZoneId applicationZone;

    /**
     * 기본 생성자 - UTC 기준 시간 사용
     */
    public DefaultTimeProvider() {
        this(Clock.systemUTC(), ZoneOffset.UTC);
    }

    /**
     * 테스트용 생성자 - 커스텀 Clock과 시간대 설정 가능
     * @param clock 사용할 Clock 인스턴스
     * @param applicationZone 애플리케이션 기본 시간대
     */
    public DefaultTimeProvider(Clock clock, ZoneId applicationZone) {
        this.clock = clock;
        this.applicationZone = applicationZone;
    }

    @Override
    public LocalDateTime now() {
        // 애플리케이션 기본 시간대 기준으로 LocalDateTime 반환
        return LocalDateTime.now(clock.withZone(applicationZone));
    }

    @Override
    public ZonedDateTime nowInZone(ZoneId zoneId) {
        return ZonedDateTime.now(clock.withZone(zoneId));
    }

    @Override
    public ZonedDateTime nowInUtc() {
        return ZonedDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    @Override
    public ZonedDateTime nowInSystemZone() {
        return ZonedDateTime.now(clock.withZone(ZoneId.systemDefault()));
    }

    @Override
    public ZoneId getApplicationZone() {
        return applicationZone;
    }

    @Override
    public Clock getClock() {
        return clock;
    }

    /**
     * 특정 시간대로 변환된 현재 시간을 LocalDateTime으로 반환
     * @param zoneId 변환할 시간대
     * @return 지정된 시간대의 LocalDateTime
     */
    public LocalDateTime nowInZoneAsLocalDateTime(ZoneId zoneId) {
        return nowInZone(zoneId).toLocalDateTime();
    }

    /**
     * UTC 기준 현재 시간을 LocalDateTime으로 반환
     * @return UTC 기준 LocalDateTime
     */
    public LocalDateTime nowInUtcAsLocalDateTime() {
        return nowInUtc().toLocalDateTime();
    }

    /**
     * 디버깅용 현재 시간 정보 출력
     * @return 시간대별 현재 시간 정보
     */
    public String getTimeInfo() {
        ZonedDateTime utc = nowInUtc();
        ZonedDateTime app = nowInZone(applicationZone);
        ZonedDateTime system = nowInSystemZone();
        
        return String.format(
            "TimeProvider Info - UTC: %s, App(%s): %s, System(%s): %s",
            utc, applicationZone, app, ZoneId.systemDefault(), system
        );
    }
}