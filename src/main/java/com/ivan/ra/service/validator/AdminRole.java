package com.ivan.ra.service.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ FIELD })
@Retention(RUNTIME)
@Constraint(validatedBy = AdminRoleValidator.class)
@Documented
public @interface AdminRole {

    String message() default "valid role names must be present";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
