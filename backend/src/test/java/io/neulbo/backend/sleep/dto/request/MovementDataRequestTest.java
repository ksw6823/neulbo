package io.neulbo.backend.sleep.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class MovementDataRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("정상적인 움직임 데이터 요청 - 검증 통과")
    void shouldPassValidation_WhenValidMovementData() {
        // given
        List<MovementDataRequest.MovementEntry> entries = List.of(
            createValidMovementEntry(),
            createValidMovementEntry()
        );
        MovementDataRequest request = new MovementDataRequest(entries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("null 움직임 데이터 목록 - 검증 실패")
    void shouldFailValidation_WhenMovementDataIsNull() {
        // given
        MovementDataRequest request = new MovementDataRequest(null);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("움직임 데이터 목록은 필수입니다");
    }

    @Test
    @DisplayName("빈 움직임 데이터 목록 - 검증 실패")
    void shouldFailValidation_WhenMovementDataIsEmpty() {
        // given
        MovementDataRequest request = new MovementDataRequest(Collections.emptyList());

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("1개 이상 1000개 이하여야 합니다");
    }

    @Test
    @DisplayName("1개 움직임 데이터 - 검증 통과")
    void shouldPassValidation_WhenMovementDataHasOneEntry() {
        // given
        List<MovementDataRequest.MovementEntry> entries = List.of(createValidMovementEntry());
        MovementDataRequest request = new MovementDataRequest(entries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("1000개 움직임 데이터 - 검증 통과")
    void shouldPassValidation_WhenMovementDataHasMaxEntries() {
        // given
        List<MovementDataRequest.MovementEntry> entries = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            entries.add(createValidMovementEntry());
        }
        MovementDataRequest request = new MovementDataRequest(entries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("1001개 움직임 데이터 - 검증 실패")
    void shouldFailValidation_WhenMovementDataExceedsMaxSize() {
        // given
        List<MovementDataRequest.MovementEntry> entries = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            entries.add(createValidMovementEntry());
        }
        MovementDataRequest request = new MovementDataRequest(entries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("1개 이상 1000개 이하여야 합니다");
    }

    @Test
    @DisplayName("MovementEntry 필드 검증 - null timestamp")
    void shouldFailValidation_WhenMovementEntryHasNullTimestamp() {
        // given
        MovementDataRequest.MovementEntry invalidEntry = 
            new MovementDataRequest.MovementEntry(null, 1.0, 2.0, 3.0);
        List<MovementDataRequest.MovementEntry> entries = List.of(invalidEntry);
        MovementDataRequest request = new MovementDataRequest(entries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("타임스탬프는 필수입니다");
    }

    @Test
    @DisplayName("MovementEntry 필드 검증 - null accelerationX")
    void shouldFailValidation_WhenMovementEntryHasNullAccelerationX() {
        // given
        MovementDataRequest.MovementEntry invalidEntry = 
            new MovementDataRequest.MovementEntry(LocalDateTime.now(), null, 2.0, 3.0);
        List<MovementDataRequest.MovementEntry> entries = List.of(invalidEntry);
        MovementDataRequest request = new MovementDataRequest(entries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("X축 가속도는 필수입니다");
    }

    @Test
    @DisplayName("대용량 배치 크기 제한 테스트")
    void shouldRejectExcessivelyLargeBatches() {
        // given
        int excessiveSize = 5000; // 1000 제한을 크게 초과
        List<MovementDataRequest.MovementEntry> largeEntries = new ArrayList<>();
        for (int i = 0; i < excessiveSize; i++) {
            largeEntries.add(createValidMovementEntry());
        }
        MovementDataRequest request = new MovementDataRequest(largeEntries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("movementData");
        assertThat(violations.iterator().next().getMessage())
                .contains("1개 이상 1000개 이하여야 합니다");
    }

    @Test
    @DisplayName("미래 타임스탬프 - 검증 실패")
    void shouldFailValidation_WhenTimestampIsInFuture() {
        // given
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        MovementDataRequest.MovementEntry futureEntry = 
            new MovementDataRequest.MovementEntry(futureTime, 1.0, 2.0, 3.0);
        List<MovementDataRequest.MovementEntry> entries = List.of(futureEntry);
        MovementDataRequest request = new MovementDataRequest(entries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("미래 시간일 수 없습니다");
    }

    @Test
    @DisplayName("너무 먼 과거 타임스탬프 - 검증 실패")
    void shouldFailValidation_WhenTimestampIsTooFarInPast() {
        // given
        LocalDateTime farPastTime = LocalDateTime.now().minusHours(73); // 72시간 초과
        MovementDataRequest.MovementEntry pastEntry = 
            new MovementDataRequest.MovementEntry(farPastTime, 1.0, 2.0, 3.0);
        List<MovementDataRequest.MovementEntry> entries = List.of(pastEntry);
        MovementDataRequest request = new MovementDataRequest(entries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("72시간 이전의 움직임 데이터는 허용되지 않습니다");
    }

    @Test
    @DisplayName("유효한 과거 타임스탬프 - 검증 통과")
    void shouldPassValidation_WhenTimestampIsValidPast() {
        // given
        LocalDateTime validPastTime = LocalDateTime.now().minusHours(12); // 12시간 전
        MovementDataRequest.MovementEntry validEntry = 
            new MovementDataRequest.MovementEntry(validPastTime, 1.0, 2.0, 3.0);
        List<MovementDataRequest.MovementEntry> entries = List.of(validEntry);
        MovementDataRequest request = new MovementDataRequest(entries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("경계값 테스트 - 정확히 72시간 전")
    void shouldPassValidation_WhenTimestampIsExactly72HoursAgo() {
        // given
        LocalDateTime boundaryTime = LocalDateTime.now().minusHours(72); // 정확히 72시간 전
        MovementDataRequest.MovementEntry boundaryEntry = 
            new MovementDataRequest.MovementEntry(boundaryTime, 1.0, 2.0, 3.0);
        List<MovementDataRequest.MovementEntry> entries = List.of(boundaryEntry);
        MovementDataRequest request = new MovementDataRequest(entries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("현재 시간 - 검증 통과")
    void shouldPassValidation_WhenTimestampIsNow() {
        // given
        LocalDateTime now = LocalDateTime.now();
        MovementDataRequest.MovementEntry currentEntry = 
            new MovementDataRequest.MovementEntry(now, 1.0, 2.0, 3.0);
        List<MovementDataRequest.MovementEntry> entries = List.of(currentEntry);
        MovementDataRequest request = new MovementDataRequest(entries);

        // when
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("성능 테스트 - 최대 허용 크기에서의 검증 속도")
    void shouldValidateMaxSizeQuickly() {
        // given
        List<MovementDataRequest.MovementEntry> maxEntries = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            maxEntries.add(createValidMovementEntry());
        }
        MovementDataRequest request = new MovementDataRequest(maxEntries);

        // when
        long startTime = System.currentTimeMillis();
        Set<ConstraintViolation<MovementDataRequest>> violations = validator.validate(request);
        long endTime = System.currentTimeMillis();

        // then
        assertThat(violations).isEmpty();
        assertThat(endTime - startTime).isLessThan(1000); // 1초 이내에 검증 완료
    }

    private MovementDataRequest.MovementEntry createValidMovementEntry() {
        return new MovementDataRequest.MovementEntry(
            LocalDateTime.now(),
            1.0,
            2.0,
            3.0
        );
    }
}