package is442t1.studybuddy.common.exception;

/** The request breaks a validation rule (HTTP 400). */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }
}
