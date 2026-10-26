package is442t1.studybuddy.matching.strategy;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import is442t1.studybuddy.matching.MatchResult;
import is442t1.studybuddy.model.enums.MatchingStrategy;

/**
 * Balanced matching: ranks purely by the overall score, which already weighs
 * every enabled criterion by its configured weight.
 */
@Component
public class BalancedRankingStrategy implements RankingStrategy {

    @Override
    public MatchingStrategy type() {
        return MatchingStrategy.BALANCED;
    }

    @Override
    public Comparator<MatchResult> comparator() {
        return BY_SCORE.thenComparing(BY_NAME);
    }
}
