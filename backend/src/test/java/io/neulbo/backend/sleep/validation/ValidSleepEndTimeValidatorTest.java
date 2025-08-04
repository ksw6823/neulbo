package io.neulbo.backend.sleep.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidSleepEndTimeValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("현재 시간이면 검증 통과")
    void shouldPassValidation_WhenCurrentTime() {
        // given
        TestRequest request = new TestRequest(LocalDateTime.now());

        // when
        Set<ConstraintViolation<TestRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("1시간 전이면 검증 통과")
    void shouldPassValidation_WhenOneHourAgo() {
        // given
        TestRequest request = new TestRequest(LocalDateTime.now().minusHours(1));

        // when
        Set<ConstraintViolation<TestRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("3일 전이면 검증 통과")
    void shouldPassValidation_WhenThreeDaysAgo() {
        // given
        TestRequest request = new TestRequest(LocalDateTime.now().minusDays(3));

        // when
        Set<ConstraintViolation<TestRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("미래 시간이면 검증 실패")
    void shouldFailValidation_WhenFutureTime() {
        // given
        TestRequest request = new TestRequest(LocalDateTime.now().plusHours(1));

        // when
        Set<ConstraintViolation<TestRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("미래 시간일 수 없습니다");
    }

    @Test
    @DisplayName("8일 전이면 검증 실패")
    void shouldFailValidation_WhenEightDaysAgo() {
        // given
        TestRequest request = new TestRequest(LocalDateTime.now().minusDays(8));

        // when
        Set<ConstraintViolation<TestRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("7일 이전일 수 없습니다");
    }

    @Test
    @DisplayName("null 값이면 검증 통과 (NotNull에서 처리)")
    void shouldPassValidation_WhenNull() {
        // given
        TestRequest request = new TestRequest(null);

        // when
        Set<ConstraintViolation<TestRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    /**
     * 테스트용 요청 클래스
     */
    static class TestRequest {
        @ValidSleepEndTime(maxPastDays = 7)
        private LocalDateTime sleepEndTime;

        public TestRequest(LocalDateTime sleepEndTime) {
            this.sleepEndTime = sleepEndTime;
        }

        public LocalDateTime getSleepEndTime() {
            return sleepEndTime;
        }
    }
}