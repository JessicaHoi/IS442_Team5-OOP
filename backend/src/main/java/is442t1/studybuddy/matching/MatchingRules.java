package is442t1.studybuddy.matching;

import java.util.Map;

import is442t1.studybuddy.model.enums.MatchingStrategy;

/**
 * The matching settings in force for one search: the ranking strategy and the
 * weight of each criterion. A weight of 0 means the criterion is switched off.
 *
 * @param strategy the ranking strategy
 * @param weights the weight assigned to each criterion
 */
public record MatchingRules(MatchingStrategy strategy, Map<MatchingCriterion, Integer> weights) {

    /**
     * Creates rules with an immutable copy of the criterion weights.
     *
     * @param strategy the ranking strategy
     * @param weights the weight assigned to each criterion
     */
    public MatchingRules {
        weights = Map.copyOf(weights);
    }

    /**
     * Returns the weight for a criterion, or zero when it is absent.
     *
     * @param criterion the criterion to look up
     * @return its weight, or zero
     */
    public int weightOf(MatchingCriterion criterion) {
        return weights.getOrDefault(criterion, 0);
    }

    /**
     * Calculates the weighted average of the enabled criteria.
     *
     * @param breakdown the criterion scores
     * @return the rounded score from 0 to 100, or zero when total weight is zero
     */
    public int score(MatchScoreBreakdown breakdown) {
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight == 0) {
            return 0;
        }
        double weighted = weights.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().scoreIn(breakdown) * entry.getValue())
                .sum();
        return (int) Math.round(weighted / totalWeight * 100);
    }
}
