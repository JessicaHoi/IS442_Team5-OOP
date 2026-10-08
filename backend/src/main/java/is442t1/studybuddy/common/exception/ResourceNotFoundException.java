package is442t1.studybuddy.common.exception;

/** The requested resource does not exist (HTTP 404). */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
