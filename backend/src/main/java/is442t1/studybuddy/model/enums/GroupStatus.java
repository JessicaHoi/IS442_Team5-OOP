package is442t1.studybuddy.model.enums;

/**
 * Study-group lifecycle: OPEN or permanently CLOSED. Closing rejects pending join requests and
 *     prevents ordinary membership and availability changes.
 */
public enum GroupStatus {
    OPEN,
    CLOSED
}
