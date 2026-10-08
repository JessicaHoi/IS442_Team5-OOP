package is442t1.studybuddy.common.exception;

/**
 * Base class for business errors. The message is safe to show to the user,
 * so the API returns it unchanged.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
