package com.example.scheduler.validator.specialtyRole;

import com.example.scheduler.enums.ERole;
import com.example.scheduler.enums.ESpecialty;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;

public class SpecialtyRoleMatchValidator implements ConstraintValidator<SpecialtyRoleMatch, Object> {
    private String firstField;
    private String  secondField;
    private String message;

    @Override
    public void initialize(SpecialtyRoleMatch constraintAnnotation) {
        this.firstField = constraintAnnotation.first();
        this.secondField = constraintAnnotation.second();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if(value == null) return true;
        try {
            Long roleId = (Long) getFieldValue(value, firstField);
            Long specialtyId = (Long) getFieldValue(value, secondField);
            if(roleId == null) return true;
            boolean isDoctor = roleId.equals(ERole.DOCTOR.getId());
            boolean existsSpecialty = specialtyId != null;
            if (isDoctor && !existsSpecialty) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(secondField)
                        .addConstraintViolation();
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Object getFieldValue(Object object, String fieldName) throws Exception {
        Class<?> clazz = object.getClass();
        try {
            Method recordMethod = clazz.getMethod(fieldName);
            return recordMethod.invoke(object);
        } catch (NoSuchMethodException ignored) {}
        String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        Method getterMethod = clazz.getMethod(getterName);
        return getterMethod.invoke(object);
    }
}
