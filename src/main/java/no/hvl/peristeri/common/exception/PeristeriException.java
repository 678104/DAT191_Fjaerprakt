package no.hvl.peristeri.common.exception;

/**
 * Base exception class for all application-specific exceptions in the Peristeri application.
 */
public class PeristeriException extends RuntimeException {
    
    public PeristeriException(String message) {
        super(message);
    }
    
    public PeristeriException(String message, Throwable cause) {
        super(message, cause);
    }
}
