package dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An optional account name: null or blank means "no name". Otherwise at most MAX_LENGTH characters once
 * surrounding whitespace is stripped, and no control characters anywhere (tabs and newlines included).
 * AccountService.normalizeAccountName applies the same stripping before the name is stored.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AccountName.Validator.class)
public @interface AccountName {
    int MAX_LENGTH = 40;

    String message() default "Account name is invalid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<AccountName, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            String reason = null;
            if (value.codePoints().anyMatch(c -> Character.getType(c) == Character.CONTROL)) {
                reason = "Account name must not contain control characters";
            } else {
                String stripped = value.strip();
                // Count characters, not UTF-16 units, so an emoji counts once
                if (stripped.codePointCount(0, stripped.length()) > MAX_LENGTH) {
                    reason = "Account name must be at most " + MAX_LENGTH + " characters";
                }
            }
            if (reason == null) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(reason).addConstraintViolation();
            return false;
        }
    }
}
