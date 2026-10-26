package is442t1.studybuddy.matching.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import is442t1.studybuddy.model.enums.MatchingStrategy;

/** Finds the {@link RankingStrategy} for a configured {@link MatchingStrategy}. */
@Component
public class RankingStrategyResolver {

    private final Map<MatchingStrategy, RankingStrategy> strategies = new EnumMap<>(MatchingStrategy.class);

    public RankingStrategyResolver(List<RankingStrategy> available) {
        available.forEach(strategy -> strategies.put(strategy.type(), strategy));
    }

    public boolean supports(MatchingStrategy type) {
        return strategies.containsKey(type);
    }

    /** The strategy for {@code type}, or balanced matching if that strategy is not implemented. */
    public RankingStrategy resolve(MatchingStrategy type) {
        RankingStrategy strategy = strategies.get(type);
        return strategy != null ? strategy : strategies.get(MatchingStrategy.BALANCED);
    }
}
