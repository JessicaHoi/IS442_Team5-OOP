package is442t1.studybuddy.scheduling;

import java.time.DayOfWeek;

/** One hour of one weekday and how many people are free then. */
public record HeatmapCell(DayOfWeek day, int hour, int freeCount) { }