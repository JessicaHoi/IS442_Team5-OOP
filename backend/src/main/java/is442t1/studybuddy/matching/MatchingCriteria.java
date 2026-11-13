package is442t1.studybuddy.matching;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Persisted criterion API key and weight inside a matching configuration. A zero weight represents
 *     a disabled criterion.
 */
@Embeddable
public record MatchingCriteria(
        @Column(nullable = false)
        String criteriaName,

        @Column(nullable = false)
        int weight
) {
}
