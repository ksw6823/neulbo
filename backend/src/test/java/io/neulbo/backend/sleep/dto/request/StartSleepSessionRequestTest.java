package io.neulbo.backend.sleep.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class StartSleepSessionRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("정상적인 수면 시작 요청 - 검증 통과")
    void shouldPassValidation_WhenValidSleepStartRequest() {
        // given
        StartSleepSessionRequest request = new StartSleepSessionRequest(
            LocalDateTime.now().minusMinutes(5), // 5분 전
            480, // 8시간
            "테스트 수면"
        );

        // when
        Set<ConstraintViolation<StartSleepSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("null 수면 시작 시간 - 검증 실패")
    void shouldFailValidation_WhenSleepStartTimeIsNull() {
        // given
        StartSleepSessionRequest request = new StartSleepSessionRequest(
            null,
            480,
            "테스트 수면"
        );

        // when
        Set<ConstraintViolation<StartSleepSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("수면 시작 시간은 필수입니다");
    }

    @Test
    @DisplayName("미래 수면 시작 시간 - 검증 실패")
    void shouldFailValidation_WhenSleepStartTimeIsInFuture() {
        // given
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        StartSleepSessionRequest request = new StartSleepSessionRequest(
            futureTime,
            480,
            "미래 수면"
        );

        // when
        Set<ConstraintViolation<StartSleepSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("미래 시간일 수 없습니다");
    }

    @Test
    @DisplayName("너무 먼 과거 수면 시작 시간 - 검증 실패")
    void shouldFailValidation_WhenSleepStartTimeIsTooFarInPast() {
        // given
        LocalDateTime farPastTime = LocalDateTime.now().minusHours(25); // 25시간 전 (24시간 초과)
        StartSleepSessionRequest request = new StartSleepSessionRequest(
            farPastTime,
            480,
            "과거 수면"
        );

        // when
        Set<ConstraintViolation<StartSleepSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("24시간 이전의 수면 시작 시간은 허용되지 않습니다");
    }

    @Test
    @DisplayName("유효한 과거 수면 시작 시간 - 검증 통과")
    void shouldPassValidation_WhenSleepStartTimeIsValidPast() {
        // given
        LocalDateTime validPastTime = LocalDateTime.now().minusHours(12); // 12시간 전
        StartSleepSessionRequest request = new StartSleepSessionRequest(
            validPastTime,
            480,
            "유효한 과거 수면"
        );

        // when
        Set<ConstraintViolation<StartSleepSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("경계값 테스트 - 정확히 24시간 전")
    void shouldPassValidation_WhenSleepStartTimeIsExactly24HoursAgo() {
        // given
        LocalDateTime boundaryTime = LocalDateTime.now().minusHours(24); // 정확히 24시간 전
        StartSleepSessionRequest request = new StartSleepSessionRequest(
            boundaryTime,
            480,
            "경계값 수면"
        );

        // when
        Set<ConstraintViolation<StartSleepSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("현재 시간 - 검증 통과")
    void shouldPassValidation_WhenSleepStartTimeIsNow() {
        // given
        LocalDateTime now = LocalDateTime.now();
        StartSleepSessionRequest request = new StartSleepSessionRequest(
            now,
            480,
            "현재 수면 시작"
        );

        // when
        Set<ConstraintViolation<StartSleepSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("예상 수면 시간 최소값 검증 - 30분 미만 실패")
    void shouldFailValidation_WhenIntendedSleepDurationIsTooShort() {
        // given
        StartSleepSessionRequest request = new StartSleepSessionRequest(
            LocalDateTime.now().minusMinutes(5),
            20, // 20분 (최소 30분 미만)
            "짧은 수면"
        );

        // when
        Set<ConstraintViolation<StartSleepSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("예상 수면 시간은 최소 30분 이상이어야 합니다");
    }

    @Test
    @DisplayName("예상 수면 시간 최대값 검증 - 12시간 초과 실패")
    void shouldFailValidation_WhenIntendedSleepDurationIsTooLong() {
        // given
        StartSleepSessionRequest request = new StartSleepSessionRequest(
            LocalDateTime.now().minusMinutes(5),
            800, // 800분 (12시간 초과)
            "긴 수면"
        );

        // when
        Set<ConstraintViolation<StartSleepSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("예상 수면 시간은 최대 12시간까지 가능합니다");
    }

    @Test
    @DisplayName("다중 검증 실패 - 미래 시간과 잘못된 수면 시간")
    void shouldFailValidation_WhenMultipleConstraintsViolated() {
        // given
        StartSleepSessionRequest request = new StartSleepSessionRequest(
            LocalDateTime.now().plusHours(1), // 미래 시간
            20, // 너무 짧은 수면 시간
            "다중 오류"
        );

        // when
        Set<ConstraintViolation<StartSleepSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("미래 시간일 수 없습니다")
                .contains("예상 수면 시간은 최소 30분 이상이어야 합니다");
    }

    @Test
    @DisplayName("실제 수면 시나리오 - 잠자리에 들기 직전")
    void shouldPassValidation_WhenRealisticSleepScenario() {
        // given - 실제 사용자가 잠자리에 들면서 앱에서 수면을 시작하는 시나리오
        LocalDateTime bedTime = LocalDateTime.now().minusMinutes(2); // 2분 전에 잠자리에 듦
        StartSleepSessionRequest request = new StartSleepSessionRequest(
            bedTime,
            480, // 8시간 예정
            "숙면을 위한 수면 세션"
        );

        // when
        Set<ConstraintViolation<StartSleepSessionRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }
}