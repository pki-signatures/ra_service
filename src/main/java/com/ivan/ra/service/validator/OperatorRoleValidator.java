package com.ivan.ra.service.validator;

import com.ivan.ra.service.constants.RaServiceConstants;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

public class OperatorRoleValidator implements ConstraintValidator<OperatorRole, String[]> {

    @Override
    public boolean isValid(String[] values, ConstraintValidatorContext context) {
        if (values == null || values.length == 0) {
            return true;
        }

        List<String> rolesList = new ArrayList<>();
        rolesList.add(RaServiceConstants.CREATE_REQUESTS);
        rolesList.add(RaServiceConstants.UPDATE_REQUESTS);
        rolesList.add(RaServiceConstants.READ_REQUESTS);
        rolesList.add(RaServiceConstants.APPROVE_REQUESTS);
        rolesList.add(RaServiceConstants.REJECT_REQUESTS);

        for (String role: values) {
            if (!rolesList.contains(role)) {
                return false;
            }
        }
        return true;
    }
}
