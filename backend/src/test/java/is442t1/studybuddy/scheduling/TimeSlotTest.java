package is442t1.studybuddy.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import is442t1.studybuddy.model.TimeSlot;

class TimeSlotTest {

    private static TimeSlot tue(int startHour, int endHour) {
        return new TimeSlot(DayOfWeek.TUESDAY, LocalTime.of(startHour, 0), LocalTime.of(endHour, 0));
    }

    @Test
    void intersectReturnsTheSharedPart() {
        assertEquals(tue(19, 21), tue(18, 21).intersect(tue(19, 22)).orElseThrow());
    }

    @Test
    void intersectIsEmptyOnDifferentDays() {
        TimeSlot monday = new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(21, 0));
        assertTrue(tue(18, 21).intersect(monday).isEmpty());
    }

    @Test
    void intersectIsEmptyWhenSlotsOnlyTouch() {
        assertTrue(tue(10, 12).intersect(tue(12, 14)).isEmpty());
    }

    @Test
    void durationIsEndMinusStart() {
        assertEquals(Duration.ofHours(3), tue(18, 21).duration());
    }
}