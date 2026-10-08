package is442t1.studybuddy.common.exception;

/** The caller is signed in but may not perform the action (HTTP 403). */
public class PermissionDeniedException extends DomainException {

    public PermissionDeniedException(String message) {
        super(message);
    }
}
