package is442t1.studybuddy.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.model.enums.MatchingStrategy;

/** Persisted ranking strategy and criterion weights. */
@Entity
@Table(name = "matching_configuration")
public class MatchingConfiguration extends BaseEntity {

    /** Creates a configuration with the balanced strategy and no criterion weights. */
    public MatchingConfiguration() {
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchingStrategy strategy = MatchingStrategy.BALANCED;

    @ElementCollection
    @CollectionTable(name = "matching_criteria", joinColumns = @JoinColumn(name = "matching_configuration_id"))
    private List<MatchingCriteria> criteria = new ArrayList<>();

    /**
     * The configuration used until an administrator changes it. Keep the weights
     * in line with DEFAULT_CRITERIA in backend/supabase/generate_seed_sql.py.
     *
     * @return a configuration with the default strategy and weights
     */
    public static MatchingConfiguration defaults() {
        MatchingConfiguration configuration = new MatchingConfiguration();
        configuration.criteria.addAll(List.of(
                new MatchingCriteria("course", 30),
                new MatchingCriteria("availability", 25),
                new MatchingCriteria("studyMode", 15),
                new MatchingCriteria("studyGoal", 20),
                new MatchingCriteria("groupSize", 10)));
        return configuration;
    }

    /**
     * Returns the configured strategy.
     *
     * @return the configured strategy
     */
    public MatchingStrategy getStrategy() {
        return strategy;
    }

    /**
     * Sets the ranking strategy.
     *
     * @param strategy the ranking strategy
     */
    public void setStrategy(MatchingStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns an unmodifiable view of the configured criteria.
     *
     * @return an unmodifiable view of the configured criteria
     */
    public List<MatchingCriteria> getCriteria() {
        return Collections.unmodifiableList(criteria);
    }

    /**
     * Replaces every criterion weight.
     *
     * @param newCriteria the replacement criteria
     */
    public void replaceCriteria(List<MatchingCriteria> newCriteria) {
        criteria.clear();
        criteria.addAll(newCriteria);
    }
}
