package io.neulbo.backend.sleep.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidMovementTimestampValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMovementTimestamp {
    String message() default "움직임 데이터의 타임스탬프가 유효하지 않습니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int maxPastHours() default 72; // 최대 과거 허용 시간 (기본 3일)
}