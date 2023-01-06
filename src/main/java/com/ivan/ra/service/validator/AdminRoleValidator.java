package com.ivan.ra.service.validator;

import com.ivan.ra.service.constants.RaServiceConstants;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

public class AdminRoleValidator implements ConstraintValidator<AdminRole, String[]> {

    @Override
    public boolean isValid(String[] values, ConstraintValidatorContext context) {
        if (values == null || values.length == 0) {
            return true;
        }

        List<String> rolesList = new ArrayList<>();
        rolesList.add(RaServiceConstants.CREATE_ADMINS);
        rolesList.add(RaServiceConstants.READ_ADMINS);
        rolesList.add(RaServiceConstants.UPDATE_ADMINS);
        rolesList.add(RaServiceConstants.CREATE_OPERATORS);
        rolesList.add(RaServiceConstants.READ_OPERATORS);
        rolesList.add(RaServiceConstants.UPDATE_OPERATORS);
        rolesList.add(RaServiceConstants.CREATE_RELYING_PARTIES);
        rolesList.add(RaServiceConstants.READ_RELYING_PARTIES);
        rolesList.add(RaServiceConstants.UPDATE_RELYING_PARTIES);

        for (String role: values) {
            if (!rolesList.contains(role)) {
                return false;
            }
        }
        return true;
    }
}
