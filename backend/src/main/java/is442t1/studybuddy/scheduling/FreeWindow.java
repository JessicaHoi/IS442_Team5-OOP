package is442t1.studybuddy.scheduling;
import is442t1.studybuddy.model.TimeSlot;

/**
 * A meeting window and the number of people available during it.
 *
 * @param slot the meeting window
 * @param freeCount the number of people available
 */
public record FreeWindow(TimeSlot slot, int freeCount) { }