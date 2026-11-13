package is442t1.studybuddy.model.enums;

/**
 * Persisted request lifecycle: PENDING, ACCEPTED or REJECTED. Buddy-response JSON maps REJECTED to
 *     DECLINED.
 */
public enum RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
