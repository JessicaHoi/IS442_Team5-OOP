package is442t1.studybuddy.auth;

import java.util.Optional;

/** Reads the token out of an {@code Authorization: Bearer <token>} header. */
final class BearerToken {

    private static final String PREFIX = "Bearer ";

    private BearerToken() {
    }

    static Optional<String> extract(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
