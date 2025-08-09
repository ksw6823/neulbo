package io.neulbo.backend.sleep.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 수면 종료 시간 유효성 검증 구현체
 * 시간대 인식 TimeProvider를 사용하여 일관된 시간 처리
 */
@Component
public class ValidSleepEndTimeValidator implements ConstraintValidator<ValidSleepEndTime, LocalDateTime> {
    
    private int maxPastDays;
    
    @Autowired
    TimeProvider timeProvider; // package-private for testing
    
    @Override
    public void initialize(ValidSleepEndTime constraintAnnotation) {
        this.maxPastDays = constraintAnnotation.maxPastDays();
    }
    
    @Override
    public boolean isValid(LocalDateTime sleepEndTime, ConstraintValidatorContext context) {
        if (sleepEndTime == null) {
            return true; // @NotNull에서 처리
        }
        
        // 시간대 인식 현재 시간 사용
        LocalDateTime now = timeProvider.now();
        LocalDateTime maxPastDateTime = now.minusDays(maxPastDays);
        
        // 미래 시간 체크
        if (sleepEndTime.isAfter(now)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "수면 종료 시간은 미래 시간일 수 없습니다"
            ).addConstraintViolation();
            return false;
        }
        
        // 너무 과거 시간 체크
        if (sleepEndTime.isBefore(maxPastDateTime)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("수면 종료 시간은 %d일 이전일 수 없습니다", maxPastDays)
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}