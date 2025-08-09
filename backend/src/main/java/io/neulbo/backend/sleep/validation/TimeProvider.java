package io.neulbo.backend.sleep.validation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 시간대 인식 시간 제공 추상화 인터페이스
 * 테스트 가능하고 일관된 시간대 처리를 위한 abstraction
 */
public interface TimeProvider {

    /**
     * 현재 시간을 LocalDateTime으로 반환 (시간대 고려)
     * @return 현재 LocalDateTime
     */
    LocalDateTime now();

    /**
     * 지정된 시간대의 현재 시간을 ZonedDateTime으로 반환
     * @param zoneId 시간대
     * @return 지정된 시간대의 현재 ZonedDateTime
     */
    ZonedDateTime nowInZone(ZoneId zoneId);

    /**
     * UTC 기준 현재 시간을 ZonedDateTime으로 반환
     * @return UTC 기준 현재 ZonedDateTime
     */
    ZonedDateTime nowInUtc();

    /**
     * 시스템 기본 시간대의 현재 시간을 ZonedDateTime으로 반환
     * @return 시스템 기본 시간대의 현재 ZonedDateTime
     */
    ZonedDateTime nowInSystemZone();

    /**
     * 애플리케이션 기본 시간대 반환
     * @return 애플리케이션 기본 시간대
     */
    ZoneId getApplicationZone();

    /**
     * 내부 Clock 인스턴스 반환 (테스트 목적)
     * @return Clock 인스턴스
     */
    Clock getClock();
}