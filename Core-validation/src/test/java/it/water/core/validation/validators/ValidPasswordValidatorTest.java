package it.water.core.validation.validators;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ValidPasswordValidatorTest {

    @Test
    void validPasswordReturnsTrue() {
        ValidPasswordValidator validator = new ValidPasswordValidator();
        ConstraintValidatorContext context = Mockito.mock(ConstraintValidatorContext.class);

        boolean valid = validator.isValid("ValidPassword1!", context);

        Assertions.assertTrue(valid);
        Mockito.verify(context, Mockito.never()).buildConstraintViolationWithTemplate(Mockito.anyString());
    }

    @Test
    void invalidPasswordBuildsConstraintViolationAndReturnsFalse() {
        ValidPasswordValidator validator = new ValidPasswordValidator();

        ConstraintValidatorContext context = Mockito.mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder = Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        Mockito.when(context.buildConstraintViolationWithTemplate(Mockito.anyString())).thenReturn(violationBuilder);
        Mockito.when(violationBuilder.addConstraintViolation()).thenReturn(context);

        boolean valid = validator.isValid("abc", context);

        Assertions.assertFalse(valid);
        Mockito.verify(context).buildConstraintViolationWithTemplate(Mockito.anyString());
        Mockito.verify(violationBuilder).addConstraintViolation();
        Mockito.verify(context).disableDefaultConstraintViolation();
    }
}
