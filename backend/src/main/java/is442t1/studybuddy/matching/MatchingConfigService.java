package is442t1.studybuddy.matching;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.matching.MatchingConfigDto.CriterionSetting;
import is442t1.studybuddy.matching.strategy.RankingStrategyResolver;

/**
 * Reads and updates the single matching configuration that the administrator
 * controls. Until one is saved, {@link MatchingConfiguration#defaults()} is used.
 * <p>
 * A switched-off criterion is stored with weight 0, so the database schema
 * does not need an extra column.
 */
@Service
public class MatchingConfigService {

    private final MatchingConfigurationRepository repository;
    private final RankingStrategyResolver strategyResolver;

    public MatchingConfigService(MatchingConfigurationRepository repository,
            RankingStrategyResolver strategyResolver) {
        this.repository = repository;
        this.strategyResolver = strategyResolver;
    }

    @Transactional(readOnly = true)
    public MatchingConfigDto getConfig() {
        return toDto(current());
    }

    @Transactional
    public MatchingConfigDto updateConfig(MatchingConfigDto request) {
        MatchingRules rules = toRules(request);

        MatchingConfiguration configuration = repository.findAll().stream().findFirst()
                .orElseGet(MatchingConfiguration::defaults);
        configuration.setStrategy(rules.strategy());
        configuration.replaceCriteria(rules.weights().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MatchingCriteria(entry.getKey().key(), entry.getValue()))
                .toList());
        return toDto(repository.save(configuration));
    }

    /**
     * Validates settings the administrator has not saved yet (e.g. for a preview)
     * and turns them into rules the matching engine can run.
     */
    public MatchingRules toRules(MatchingConfigDto settings) {
        if (!strategyResolver.supports(settings.strategy())) {
            throw new ValidationException("This matching strategy is not available yet.");
        }
        return new MatchingRules(settings.strategy(), validatedWeights(settings.criteria()));
    }

    /** The strategy and weights to use for a search. */
    @Transactional(readOnly = true)
    public MatchingRules currentRules() {
        MatchingConfiguration configuration = current();
        Map<MatchingCriterion, Integer> weights = new EnumMap<>(MatchingCriterion.class);
        for (MatchingCriteria criteria : configuration.getCriteria()) {
            MatchingCriterion.fromKey(criteria.criteriaName())
                    .ifPresent(criterion -> weights.put(criterion, criteria.weight()));
        }
        return new MatchingRules(configuration.getStrategy(), weights);
    }

    private MatchingConfiguration current() {
        return repository.findAll().stream().findFirst().orElseGet(MatchingConfiguration::defaults);
    }

    /** Checks every criterion is present once and at least one counts; disabled ones get weight 0. */
    private static Map<MatchingCriterion, Integer> validatedWeights(List<CriterionSetting> settings) {
        Map<MatchingCriterion, Integer> weights = new EnumMap<>(MatchingCriterion.class);
        for (CriterionSetting setting : settings) {
            MatchingCriterion criterion = MatchingCriterion.fromKey(setting.key())
                    .orElseThrow(() -> new ValidationException("Unknown matching criterion: " + setting.key()));
            if (weights.put(criterion, setting.enabled() ? setting.weight() : 0) != null) {
                throw new ValidationException("Each matching criterion can only be configured once.");
            }
        }
        if (weights.size() != MatchingCriterion.values().length) {
            throw new ValidationException("Every matching criterion must be configured.");
        }
        if (weights.values().stream().allMatch(weight -> weight == 0)) {
            throw new ValidationException("Enable at least one criterion with a weight above 0.");
        }
        return weights;
    }

    private static MatchingConfigDto toDto(MatchingConfiguration configuration) {
        Map<String, Integer> saved = new HashMap<>();
        configuration.getCriteria().forEach(criteria -> saved.put(criteria.criteriaName(), criteria.weight()));
        List<CriterionSetting> criteria = Arrays.stream(MatchingCriterion.values())
                .map(criterion -> {
                    int weight = saved.getOrDefault(criterion.key(), 0);
                    return new CriterionSetting(criterion.key(), weight > 0, weight);
                })
                .toList();
        return new MatchingConfigDto(configuration.getStrategy(), criteria);
    }
}
