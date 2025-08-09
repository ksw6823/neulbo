package io.neulbo.backend.sleep.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 수면 시작 시간 유효성 검증 구현체
 * 시간대 인식 TimeProvider를 사용하여 일관된 시간 처리
 */
@Component
public class ValidSleepStartTimeValidator implements ConstraintValidator<ValidSleepStartTime, LocalDateTime> {

    private int maxPastHours;
    
    @Autowired
    TimeProvider timeProvider; // package-private for testing

    @Override
    public void initialize(ValidSleepStartTime constraintAnnotation) {
        this.maxPastHours = constraintAnnotation.maxPastHours();
    }

    @Override
    public boolean isValid(LocalDateTime sleepStartTime, ConstraintValidatorContext context) {
        if (sleepStartTime == null) {
            return true; // @NotNull 에서 처리
        }

        // 시간대 인식 현재 시간 사용
        LocalDateTime now = timeProvider.now();

        // 미래 시간 검증
        if (sleepStartTime.isAfter(now)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("수면 시작 시간은 미래 시간일 수 없습니다")
                    .addConstraintViolation();
            return false;
        }

        // 너무 먼 과거 시간 검증
        if (ChronoUnit.HOURS.between(sleepStartTime, now) > maxPastHours) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(maxPastHours + "시간 이전의 수면 시작 시간은 허용되지 않습니다")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}