package com.ivan.ra.service.validator;

import com.ivan.ra.service.constants.RaServiceConstants;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class StatusValueCheckValidator implements ConstraintValidator<StatusValueCheck, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        return value.equals(RaServiceConstants.STATUS_ENABLED) || value.equals(RaServiceConstants.STATUS_DISABLED);
    }
}
