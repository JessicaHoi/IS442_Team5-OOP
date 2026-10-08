package is442t1.studybuddy.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import is442t1.studybuddy.common.exception.ValidationException;

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

    /** True when both slots are on the same day and share at least one minute. */
    public boolean overlaps(TimeSlot other) {
        return day == other.day && startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    /**
     * Checks a weekly availability: at least one slot, each ending after it
     * starts, and no two slots overlapping on the same day.
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
