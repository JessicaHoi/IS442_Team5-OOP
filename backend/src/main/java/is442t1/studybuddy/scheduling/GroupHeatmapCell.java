package is442t1.studybuddy.scheduling;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/** One 30-minute block of a weekday and the members available throughout it. */
public record GroupHeatmapCell(DayOfWeek day, LocalTime startTime, LocalTime endTime,
        int freeCount, List<AvailableMember> availableMembers) { }
