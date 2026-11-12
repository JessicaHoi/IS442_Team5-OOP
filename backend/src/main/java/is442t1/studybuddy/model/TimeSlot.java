package is442t1.studybuddy.model;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import is442t1.studybuddy.common.exception.ValidationException;

/**
 * A time interval on one day of the week.
 *
 * @param day the weekday
 * @param startTime the start of the interval
 * @param endTime the end of the interval
 */
@Embeddable
public record TimeSlot(
        // "day" is a reserved word in H2 db, so i used a alternative colname
        @Enumerated(EnumType.STRING)
        @Column(name = "day_of_week", nullable = false)
        DayOfWeek day,

        @Column(nullable = false)
        LocalTime startTime,

        @Column(nullable = false)
        LocalTime endTime
) {

    /**
     * Checks whether two slots on the same day share time.
     *
     * @param other the slot to compare
     * @return whether the intervals overlap
     */
    public boolean overlaps(TimeSlot other) {
        return day == other.day && startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }
    /**
     * Finds the common interval of two slots.
     *
     * @param other the slot to intersect
     * @return the shared interval, or an empty result if they do not overlap
     */
    public Optional<TimeSlot> intersect(TimeSlot other) {
        if (day != other.day) {
            return Optional.empty();
        }
        LocalTime start = startTime.isAfter(other.startTime) ? startTime : other.startTime;
        LocalTime end = endTime.isBefore(other.endTime) ? endTime : other.endTime;
        return start.isBefore(end) ? Optional.of(new TimeSlot(day, start, end)) : Optional.empty();
    }

    /**
     * Calculates the time between the interval endpoints.
     *
     * @return the interval duration
     */
    public Duration duration() {
        return Duration.between(startTime, endTime);
    }

    /**
     * Checks a weekly availability: at least one slot, each ending after it
     * starts, and no two slots overlapping on the same day.
     *
     * @param slots the availability intervals to validate
     * @throws ValidationException if the list is empty, a slot is invalid, or slots overlap
     */
    public static void requireValidAvailability(List<TimeSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new ValidationException("Add at least one availability slot.");
        }
        for (int i = 0; i < slots.size(); i++) {
            TimeSlot slot = slots.get(i);
            if (!slot.endTime.isAfter(slot.startTime)) {
                throw new ValidationException("Each time slot must end after it starts.");
            }
            for (int j = i + 1; j < slots.size(); j++) {
                if (slot.overlaps(slots.get(j))) {
                    throw new ValidationException("Two time slots overlap on the same day.");
                }
            }
        }
    }
}
