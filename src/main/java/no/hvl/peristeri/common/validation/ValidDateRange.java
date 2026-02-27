package no.hvl.peristeri.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import no.hvl.peristeri.common.DateRange;

import java.lang.annotation.*;

/**
 * Custom validation annotation for validating that a {@link DateRange} is valid.
 * Used in conjunction with the {@link DateRangeValidator} class.
 */
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {

	String message() default "Start date must be before end date";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
