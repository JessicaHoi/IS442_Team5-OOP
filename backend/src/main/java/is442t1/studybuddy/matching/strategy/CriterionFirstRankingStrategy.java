package is442t1.studybuddy.matching.strategy;

import java.util.Comparator;

import is442t1.studybuddy.matching.MatchResult;
import is442t1.studybuddy.matching.MatchingCriterion;

/**
 * Base for strategies that rank by one criterion before the overall score,
 * such as availability-first and course-first matching.
 * <p>
 * The priority criterion is grouped into quarter-point tiers (none, 25%, 50%,
 * 75%, full) so a small difference does not outweigh every other criterion;
 * the overall score then decides the order inside each tier. This is the same
 * rule the front-end mock uses.
 */
public abstract class CriterionFirstRankingStrategy implements RankingStrategy {

    private static final int TIERS = 4;

    /** The criterion this strategy ranks by first. */
    protected abstract MatchingCriterion priorityCriterion();

    @Override
    public Comparator<MatchResult> comparator() {
        Comparator<MatchResult> byPriorityTier = Comparator.comparingLong(this::priorityTier).reversed();
        return byPriorityTier.thenComparing(BY_SCORE).thenComparing(BY_NAME);
    }

    private long priorityTier(MatchResult result) {
        return Math.round(priorityCriterion().scoreIn(result.breakdown()) * TIERS);
    }
}
