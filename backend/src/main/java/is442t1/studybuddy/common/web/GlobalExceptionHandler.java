package is442t1.studybuddy.common.web;

import java.sql.SQLException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import is442t1.studybuddy.common.exception.AuthenticationException;
import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.DomainException;
import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.common.exception.ValidationException;

/**
 * Turns exceptions into the API's error format: an HTTP status plus
 * {@code {"message": "..."}}, which the front end shows to the user.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrityConflict(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String detail = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(Locale.ROOT);
        if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())
                && (detail.contains("(email") || detail.contains("email_key"))) {
            return respond(HttpStatus.CONFLICT, "Another account already uses this email address.");
        }
        return respond(HttpStatus.CONFLICT, "The change conflicts with existing data.");
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException exception) {
        return respond(statusOf(exception), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("The request is invalid.");
        return respond(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return respond(HttpStatus.BAD_REQUEST, "The request body is malformed or has an unknown value.");
    }

    /** A malformed id in the path points at nothing (404); a bad query value is a client mistake (400). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleBadArgument(MethodArgumentTypeMismatchException exception) {
        if (exception.getParameter().hasParameterAnnotation(PathVariable.class)) {
            return respond(HttpStatus.NOT_FOUND, "The requested resource does not exist.");
        }
        return respond(HttpStatus.BAD_REQUEST, "The value of '" + exception.getName() + "' is not valid.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        String message = exception.getReason() != null ? exception.getReason() : "The request could not be completed.";
        return ResponseEntity.status(exception.getStatusCode()).body(new ErrorResponse(message));
    }

    /** Last resort for bugs. Spring's own checked web exceptions (404, 405, ...) keep their default handling. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(RuntimeException exception) {
        LOG.error("Unexpected error", exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
    }

    private static HttpStatus statusOf(DomainException exception) {
        return switch (exception) {
            case ValidationException ignored -> HttpStatus.BAD_REQUEST;
            case AuthenticationException ignored -> HttpStatus.UNAUTHORIZED;
            case PermissionDeniedException ignored -> HttpStatus.FORBIDDEN;
            case ResourceNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case BusinessRuleException ignored -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
