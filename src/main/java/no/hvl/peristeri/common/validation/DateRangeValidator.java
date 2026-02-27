package no.hvl.peristeri.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import no.hvl.peristeri.common.DateRange;

/**
 * Custom validator for validating that a {@link DateRange} is valid.
 * Used by the {@link ValidDateRange} annotation.
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, DateRange> {
	@Override
	public boolean isValid(DateRange value, ConstraintValidatorContext context) {
		return value.getStartDate().isBefore(value.getEndDate());
	}
}
