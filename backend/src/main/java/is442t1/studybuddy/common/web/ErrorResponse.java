package is442t1.studybuddy.common.web;

/**
 * JSON body for errors mapped by {@link GlobalExceptionHandler}:
 * {@code {"message": "..."}}. Framework errors left to Spring's default
 * handling, such as an unsupported HTTP method, may use a different response.
 */
public record ErrorResponse(String message) {
}
