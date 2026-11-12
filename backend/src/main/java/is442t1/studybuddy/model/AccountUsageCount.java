package is442t1.studybuddy.model;

import java.util.UUID;

/** Projection of a user's usage count. */
public interface AccountUsageCount {
    /**
     * Returns the user's identifier.
     *
     * @return the user identifier
     */
    UUID getUserId();
    /**
     * Returns the number of matching usage records.
     *
     * @return the usage count
     */
    long getUsageCount();
}
