package com.ivan.ra.service.validator;

import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NoWhiteSpaceValidator implements ConstraintValidator<NoWhiteSpace, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        return !StringUtils.containsWhitespace(value);
    }
}
