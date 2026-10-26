package is442t1.studybuddy.matching;

import java.util.Map;

import is442t1.studybuddy.model.enums.MatchingStrategy;

/**
 * The matching settings in force for one search: the ranking strategy and the
 * weight of each criterion. A weight of 0 means the criterion is switched off.
 */
public record MatchingRules(MatchingStrategy strategy, Map<MatchingCriterion, Integer> weights) {

    public MatchingRules {
        weights = Map.copyOf(weights);
    }

    public int weightOf(MatchingCriterion criterion) {
        return weights.getOrDefault(criterion, 0);
    }

    /** Weighted average of the enabled criteria, as a whole number from 0 to 100. */
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
