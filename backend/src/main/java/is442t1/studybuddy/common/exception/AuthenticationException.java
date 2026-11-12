package is442t1.studybuddy.common.exception;

/** The caller is not signed in, or the credentials are wrong (HTTP 401). */
public class AuthenticationException extends DomainException {

    /**
     * Creates an authentication error with a user-facing message.
     *
     * @param message the error message
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
