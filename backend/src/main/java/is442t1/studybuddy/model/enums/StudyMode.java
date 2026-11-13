package is442t1.studybuddy.model.enums;

/**
 * Meeting-mode preference: IN_PERSON, ONLINE or EITHER. Scheduled sessions require a concrete mode
 *     and reject EITHER.
 */
public enum StudyMode {
    IN_PERSON,
    ONLINE,
    EITHER
}