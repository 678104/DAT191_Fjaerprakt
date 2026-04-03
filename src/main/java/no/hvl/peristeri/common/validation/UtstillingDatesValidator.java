package no.hvl.peristeri.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import no.hvl.peristeri.common.DateRange;
import no.hvl.peristeri.feature.utstilling.Utstilling;

import java.time.LocalDate;

public class UtstillingDatesValidator implements ConstraintValidator<ValidUtstillingDates, Utstilling> {

	@Override
	public boolean isValid(Utstilling value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}

		DateRange datoRange = value.getDatoRange();
		LocalDate paameldingStartDato = value.getPaameldingStartDato();
		LocalDate paameldingsFrist = value.getPaameldingsFrist();
		LocalDate redigeringsFrist = value.getRedigeringsFrist();
		LocalDate utstillingStart = datoRange != null ? datoRange.getStartDate() : null;
		LocalDate utstillingSlutt = datoRange != null ? datoRange.getEndDate() : null;

		if (paameldingStartDato == null || paameldingsFrist == null || redigeringsFrist == null
				|| utstillingStart == null || utstillingSlutt == null) {
			return true;
		}

		boolean gyldigRekkefolge = !paameldingStartDato.isAfter(paameldingsFrist)
				&& !paameldingsFrist.isAfter(redigeringsFrist)
				&& !redigeringsFrist.isAfter(utstillingStart)
				&& !utstillingStart.isAfter(utstillingSlutt);

		if (gyldigRekkefolge) {
			return true;
		}

		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
				.addConstraintViolation();
		return false;
	}
}

