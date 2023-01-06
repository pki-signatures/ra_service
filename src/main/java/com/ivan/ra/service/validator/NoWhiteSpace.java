package com.ivan.ra.service.validator;

import javax.validation.Constraint;
import javax.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ FIELD })
@Retention(RUNTIME)
@Constraint(validatedBy = NoWhiteSpaceValidator.class)
@Documented
public @interface NoWhiteSpace {

    String message() default "white space must not be present";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
