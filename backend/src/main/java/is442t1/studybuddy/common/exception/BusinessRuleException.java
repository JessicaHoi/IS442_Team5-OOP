package is442t1.studybuddy.common.exception;

/** The action conflicts with the current state, for example joining a full group (HTTP 409). */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
