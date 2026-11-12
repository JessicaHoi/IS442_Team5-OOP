package is442t1.studybuddy.scheduling;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * One 30-minute block of a weekday and the members available throughout it.
 *
 * @param day the weekday
 * @param startTime the start of the block
 * @param endTime the end of the block
 * @param freeCount the number of available members
 * @param availableMembers the members available throughout the block
 */
public record GroupHeatmapCell(DayOfWeek day, LocalTime startTime, LocalTime endTime,
        int freeCount, List<AvailableMember> availableMembers) { }
