package is442t1.studybuddy.model.enums;

/** Strategy used to rank study buddy candidates. */
public enum MatchingStrategy {
    /** Rank by the overall weighted score. */
    BALANCED,
    /** Prioritize availability before the overall score. */
    AVAILABILITY_FIRST,
    /** Prioritize course matching before the overall score. */
    COURSE_FIRST
}
