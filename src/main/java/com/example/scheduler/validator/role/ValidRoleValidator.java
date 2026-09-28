package com.example.scheduler.validator.role;

import com.example.scheduler.enums.ERole;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidRoleValidator implements ConstraintValidator<ValidRole, Long> {

    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        if(value == null) return true;
        return value.equals(ERole.ASSISTANT.getId()) || value.equals(ERole.DOCTOR.getId());
    }
}
