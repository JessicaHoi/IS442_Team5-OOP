package is442t1.studybuddy.matching.strategy;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import is442t1.studybuddy.matching.MatchResult;
import is442t1.studybuddy.model.enums.MatchingStrategy;

/**
 * Availability-first matching: students with the strongest timetable overlap
 * come first, and the overall score decides the order inside each level.
 * <p>
 * Overlap is grouped into quarter-point tiers of the availability score
 * (none, 25%, 50%, 75%, full) so a few minutes' difference does not outweigh
 * every other criterion. This is the same rule the front-end mock uses.
 */
@Component
public class AvailabilityFirstRankingStrategy implements RankingStrategy {

    private static final int TIERS = 4;

    @Override
    public MatchingStrategy type() {
        return MatchingStrategy.AVAILABILITY_FIRST;
    }

    @Override
    public Comparator<MatchResult> comparator() {
        Comparator<MatchResult> byAvailabilityTier = Comparator
                .comparingLong(AvailabilityFirstRankingStrategy::availabilityTier).reversed();
        return byAvailabilityTier.thenComparing(BY_SCORE).thenComparing(BY_NAME);
    }

    private static long availabilityTier(MatchResult result) {
        return Math.round(result.breakdown().availability() * TIERS);
    }
}
