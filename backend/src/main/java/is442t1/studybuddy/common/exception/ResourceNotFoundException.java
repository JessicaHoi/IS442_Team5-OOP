package is442t1.studybuddy.common.exception;

/** The requested resource does not exist (HTTP 404). */
public class ResourceNotFoundException extends DomainException {

    /**
     * Creates a not-found error with a user-facing message.
     *
     * @param message the error message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
