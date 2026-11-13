package is442t1.studybuddy.model;

import java.util.UUID;

/**
 * Repository projection associating one account ID with an aggregate usage count for administrator
 *     account summaries.
 */
public interface AccountUsageCount {
    UUID getUserId();
    long getUsageCount();
}
