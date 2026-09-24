package is442t1.studybuddy.matching;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record MatchingCriteria(
        @Column(nullable = false)
        String criteriaName,

        @Column(nullable = false)
        int weight
) {
}
