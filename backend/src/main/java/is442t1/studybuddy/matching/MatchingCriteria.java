package is442t1.studybuddy.matching;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A stored matching criterion and its configured weight.
 *
 * @param criteriaName the criterion key
 * @param weight the configured weight
 */
@Embeddable
public record MatchingCriteria(
        @Column(nullable = false)
        String criteriaName,

        @Column(nullable = false)
        int weight
) {
}
