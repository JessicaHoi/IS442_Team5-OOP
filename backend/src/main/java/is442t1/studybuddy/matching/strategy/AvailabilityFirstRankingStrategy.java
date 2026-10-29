package is442t1.studybuddy.matching.strategy;

import org.springframework.stereotype.Component;

import is442t1.studybuddy.matching.MatchingCriterion;
import is442t1.studybuddy.model.enums.MatchingStrategy;

/**
 * Availability-first matching: students with the strongest timetable overlap
 * come first, and the overall score decides the order inside each level.
 */
@Component
public class AvailabilityFirstRankingStrategy extends CriterionFirstRankingStrategy {

    @Override
    public MatchingStrategy type() {
        return MatchingStrategy.AVAILABILITY_FIRST;
    }

    @Override
    protected MatchingCriterion priorityCriterion() {
        return MatchingCriterion.AVAILABILITY;
    }
}
