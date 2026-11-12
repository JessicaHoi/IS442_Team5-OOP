package is442t1.studybuddy.common.exception;

/** The request breaks a validation rule (HTTP 400). */
public class ValidationException extends DomainException {

    /**
     * Creates a validation error with a user-facing message.
     *
     * @param message the error message
     */
    public ValidationException(String message) {
        super(message);
    }
}
