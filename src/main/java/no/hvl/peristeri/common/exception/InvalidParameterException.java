package no.hvl.peristeri.common.exception;

/**
 * Exception thrown when an invalid parameter is provided to a method.
 */
public class InvalidParameterException extends PeristeriException {
    
    public InvalidParameterException(String message) {
        super(message);
    }
    
    public InvalidParameterException(String paramName, String reason) {
        super(String.format("Invalid parameter '%s': %s", paramName, reason));
    }
    
    public InvalidParameterException(String paramName, Object value, String reason) {
        super(String.format("Invalid parameter '%s' with value '%s': %s", paramName, value, reason));
    }
}
