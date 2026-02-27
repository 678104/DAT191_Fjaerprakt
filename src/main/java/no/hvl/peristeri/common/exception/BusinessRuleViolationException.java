package no.hvl.peristeri.common.exception;

/**
 * Exception thrown when an operation violates a business rule.
 */
public class BusinessRuleViolationException extends PeristeriException {
    
    public BusinessRuleViolationException(String message) {
        super(message);
    }
    
    public BusinessRuleViolationException(String rule, String explanation) {
        super(String.format("Business rule violation: %s - %s", rule, explanation));
    }
    
    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
