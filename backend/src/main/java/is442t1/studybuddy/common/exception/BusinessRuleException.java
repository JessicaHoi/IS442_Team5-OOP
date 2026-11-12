package is442t1.studybuddy.common.exception;

/** The action conflicts with the current state, for example joining a full group (HTTP 409). */
public class BusinessRuleException extends DomainException {

    /**
     * Creates a business-rule error with a user-facing message.
     *
     * @param message the error message
     */
    public BusinessRuleException(String message) {
        super(message);
    }
}
