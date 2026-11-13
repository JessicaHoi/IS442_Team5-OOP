package is442t1.studybuddy.auth;

/**
 * Session returned by login or registration: an opaque bearer token and the user fields stored by
 *     the frontend.
 */
public record LoginResponse(String token, SessionUser user) {
}
