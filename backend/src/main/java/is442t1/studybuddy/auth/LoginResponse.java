package is442t1.studybuddy.auth;

/**
 * The session created by a successful sign-in.
 *
 * @param token the session token
 * @param user the signed-in user
 */
public record LoginResponse(String token, SessionUser user) {
}
