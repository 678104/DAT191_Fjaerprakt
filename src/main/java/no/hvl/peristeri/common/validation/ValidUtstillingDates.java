package no.hvl.peristeri.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = UtstillingDatesValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUtstillingDates {

	String message() default "Datoer må være i følgende rekkefølge: påmeldingsstart, påmeldingsfrist, redigeringsfrist, ustillingsstart og utstillingsslutt.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}

