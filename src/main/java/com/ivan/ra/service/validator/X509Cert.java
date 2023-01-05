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
@Constraint(validatedBy = X509CertValidator.class)
@Documented
public @interface X509Cert {

    String message() default "X509 certificate format must be valid";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
