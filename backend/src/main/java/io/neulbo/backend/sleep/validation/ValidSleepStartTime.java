package io.neulbo.backend.sleep.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidSleepStartTimeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSleepStartTime {
    String message() default "수면 시작 시간이 유효하지 않습니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int maxPastHours() default 24; // 최대 과거 허용 시간 (기본 1일)
}