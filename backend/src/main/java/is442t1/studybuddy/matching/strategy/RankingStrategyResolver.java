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

    /**
     * Indexes the available strategy implementations by type.
     *
     * @param available the available strategies
     */
    public RankingStrategyResolver(List<RankingStrategy> available) {
        available.forEach(strategy -> strategies.put(strategy.type(), strategy));
    }

    /**
     * Checks whether an implementation was registered for a strategy.
     *
     * @param type the strategy type
     * @return whether the type is supported
     */
    public boolean supports(MatchingStrategy type) {
        return strategies.containsKey(type);
    }

    /**
     * Resolves a strategy, falling back to balanced matching when the type is not implemented.
     *
     * @param type the strategy type to resolve
     * @return the registered strategy, or the balanced strategy
     */
    public RankingStrategy resolve(MatchingStrategy type) {
        RankingStrategy strategy = strategies.get(type);
        return strategy != null ? strategy : strategies.get(MatchingStrategy.BALANCED);
    }
}
