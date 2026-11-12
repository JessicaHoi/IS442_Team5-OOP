package is442t1.studybuddy.scheduling;

import java.time.DayOfWeek;

/**
 * One hour of one weekday and how many people are free then.
 *
 * @param day the weekday
 * @param hour the hour of the day
 * @param freeCount the number of people available
 */
public record HeatmapCell(DayOfWeek day, int hour, int freeCount) { }