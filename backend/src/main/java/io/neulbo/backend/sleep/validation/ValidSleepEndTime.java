package io.neulbo.backend.sleep.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 수면 종료 시간이 유효한지 검증하는 어노테이션
 * - 미래 시간이 아니어야 함
 * - 현재 시간 기준으로 최대 7일 전까지만 허용
 */
@Documented
@Constraint(validatedBy = ValidSleepEndTimeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSleepEndTime {
    
    String message() default "수면 종료 시간이 유효하지 않습니다";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * 허용되는 최대 과거 일수 (기본값: 7일)
     */
    int maxPastDays() default 7;
}