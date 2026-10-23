package is442t1.studybuddy.scheduling;
import is442t1.studybuddy.model.TimeSlot;

/** Meeting window & How many people are free */
public record FreeWindow(TimeSlot slot, int freeCount) { }