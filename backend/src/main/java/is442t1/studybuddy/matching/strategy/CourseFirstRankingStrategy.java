package is442t1.studybuddy.matching.strategy;

import org.springframework.stereotype.Component;

import is442t1.studybuddy.matching.MatchingCriterion;
import is442t1.studybuddy.model.enums.MatchingStrategy;

/**
 * Course-first matching: students whose study preference is for the searched
 * course come first, then those who only take it, then those who share another
 * course. The overall score decides the order inside each level.
 */
@Component
public class CourseFirstRankingStrategy extends CriterionFirstRankingStrategy {

    @Override
    public MatchingStrategy type() {
        return MatchingStrategy.COURSE_FIRST;
    }

    @Override
    protected MatchingCriterion priorityCriterion() {
        return MatchingCriterion.COURSE;
    }
}
