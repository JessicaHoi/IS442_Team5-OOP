package is442t1.studybuddy.common.exception;

/** A service StudyBuddy depends on, such as Telegram, could not do what was asked (HTTP 503). */
public class ExternalServiceException extends DomainException {

    /**
     * Creates an external-service error with a user-facing message.
     *
     * @param message the error message
     */
    public ExternalServiceException(String message) {
        super(message);
    }
}
