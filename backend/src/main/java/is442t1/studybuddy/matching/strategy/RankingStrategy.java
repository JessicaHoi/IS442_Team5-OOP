package is442t1.studybuddy.matching.strategy;

import java.util.Comparator;

import is442t1.studybuddy.matching.MatchResult;
import is442t1.studybuddy.model.enums.MatchingStrategy;

/**
 * Decides the order of match results for one {@link MatchingStrategy}.
 * Add a new strategy by implementing this interface as a Spring component;
 * {@link RankingStrategyResolver} picks it up automatically.
 */
public interface RankingStrategy {

    /** Ties are broken by name so results always come back in the same order. */
    Comparator<MatchResult> BY_NAME = Comparator.comparing(
            result -> result.student().name(), String.CASE_INSENSITIVE_ORDER);

    /** Orders results by overall score, highest first. */
    Comparator<MatchResult> BY_SCORE = Comparator.comparingInt(MatchResult::score).reversed();

    /**
     * Identifies the implemented strategy.
     *
     * @return the configured strategy type
     */
    MatchingStrategy type();

    /**
     * Provides the ordering for results.
     *
     * @return a comparator that places the best match first
     */
    Comparator<MatchResult> comparator();
}
