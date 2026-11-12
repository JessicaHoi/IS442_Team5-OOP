package is442t1.studybuddy.common.exception;

/** The caller is signed in but may not perform the action (HTTP 403). */
public class PermissionDeniedException extends DomainException {

    /**
     * Creates a permission error with a user-facing message.
     *
     * @param message the error message
     */
    public PermissionDeniedException(String message) {
        super(message);
    }
}
