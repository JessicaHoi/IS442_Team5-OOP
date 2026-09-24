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

@Entity
@Table(name = "matching_configuration")
public class MatchingConfiguration extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchingStrategy strategy = MatchingStrategy.BALANCED;

    @ElementCollection
    @CollectionTable(name = "matching_criteria", joinColumns = @JoinColumn(name = "matching_configuration_id"))
    private List<MatchingCriteria> criteria = new ArrayList<>();

    public MatchingStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(MatchingStrategy strategy) {
        this.strategy = strategy;
    }

    public List<MatchingCriteria> getCriteria() {
        return Collections.unmodifiableList(criteria);
    }
}
