package is442t1.studybuddy.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import is442t1.studybuddy.model.TimeSlot;

class SchedulingServiceTest {

    private final SchedulingService service = new SchedulingService();

    private static TimeSlot slot(DayOfWeek day, int startHour, int startMin, int endHour, int endMin) {
        return new TimeSlot(day, LocalTime.of(startHour, startMin), LocalTime.of(endHour, endMin));
    }

    private static TimeSlot slot(DayOfWeek day, int startHour, int endHour) {
        return slot(day, startHour, 0, endHour, 0);
    }

    private static int freeAt(List<HeatmapCell> cells, DayOfWeek day, int hour) {
        return cells.stream()
                .filter(c -> c.day() == day && c.hour() == hour)
                .findFirst()
                .orElseThrow()
                .freeCount();
    }

    // ---------- suggestForPair ----------

    @Test
    void pairOverlapIsLaterStartToEarlierEnd() {
        List<TimeSlot> result = service.suggestForPair(
                List.of(slot(DayOfWeek.TUESDAY, 18, 21)),
                List.of(slot(DayOfWeek.TUESDAY, 19, 22)));

        assertEquals(List.of(slot(DayOfWeek.TUESDAY, 19, 21)), result);
    }

    @Test
    void pairOnDifferentDaysHasNoOverlap() {
        List<TimeSlot> result = service.suggestForPair(
                List.of(slot(DayOfWeek.MONDAY, 18, 21)),
                List.of(slot(DayOfWeek.TUESDAY, 18, 21)));

        assertTrue(result.isEmpty());
    }

    @Test
    void pairSlotsThatOnlyTouchDoNotOverlap() {
        List<TimeSlot> result = service.suggestForPair(
                List.of(slot(DayOfWeek.FRIDAY, 10, 12)),
                List.of(slot(DayOfWeek.FRIDAY, 12, 14)));

        assertTrue(result.isEmpty());
    }

    @Test
    void pairOverlapShorterThan30MinutesIsDropped() {
        List<TimeSlot> result = service.suggestForPair(
                List.of(slot(DayOfWeek.MONDAY, 18, 0, 18, 20)),
                List.of(slot(DayOfWeek.MONDAY, 18, 0, 19, 0)));

        assertTrue(result.isEmpty());
    }

    @Test
    void pairOverlapOfExactly30MinutesIsKept() {
        List<TimeSlot> result = service.suggestForPair(
                List.of(slot(DayOfWeek.MONDAY, 18, 0, 18, 30)),
                List.of(slot(DayOfWeek.MONDAY, 18, 0, 19, 0)));

        assertEquals(1, result.size());
    }

    @Test
    void pairReturnsLongestThreeFirst() {
        List<TimeSlot> a = List.of(
                slot(DayOfWeek.MONDAY, 9, 10),          // 1h overlap
                slot(DayOfWeek.TUESDAY, 9, 12),         // 3h overlap
                slot(DayOfWeek.WEDNESDAY, 9, 11),       // 2h overlap
                slot(DayOfWeek.THURSDAY, 9, 0, 10, 30)); // 1.5h overlap
        List<TimeSlot> b = List.of(
                slot(DayOfWeek.MONDAY, 8, 13),
                slot(DayOfWeek.TUESDAY, 8, 13),
                slot(DayOfWeek.WEDNESDAY, 8, 13),
                slot(DayOfWeek.THURSDAY, 8, 13));

        List<TimeSlot> result = service.suggestForPair(a, b);

        assertEquals(3, result.size());
        assertEquals(DayOfWeek.TUESDAY, result.get(0).day());
        assertEquals(DayOfWeek.WEDNESDAY, result.get(1).day());
        assertEquals(DayOfWeek.THURSDAY, result.get(2).day());
    }

    @Test
    void pairWithEmptyAvailabilityGivesNothing() {
        assertTrue(service.suggestForPair(List.of(), List.of(slot(DayOfWeek.MONDAY, 9, 10))).isEmpty());
        assertTrue(service.suggestForPair(List.of(slot(DayOfWeek.MONDAY, 9, 10)), List.of()).isEmpty());
    }

    // ---------- heatmap ----------

    @Test
    void heatmapCoversSevenDaysBySixteenHours() {
        List<HeatmapCell> cells = service.heatmap(List.of());

        assertEquals(7 * 16, cells.size());
        assertTrue(cells.stream().allMatch(c -> c.freeCount() == 0));
    }

    @Test
    void heatmapCountsMembersPerHour() {
        List<HeatmapCell> cells = service.heatmap(List.of(
                List.of(slot(DayOfWeek.SATURDAY, 14, 16)),
                List.of(slot(DayOfWeek.SATURDAY, 15, 17))));

        assertEquals(0, freeAt(cells, DayOfWeek.SATURDAY, 13));
        assertEquals(1, freeAt(cells, DayOfWeek.SATURDAY, 14));
        assertEquals(2, freeAt(cells, DayOfWeek.SATURDAY, 15));
        assertEquals(1, freeAt(cells, DayOfWeek.SATURDAY, 16));
        assertEquals(0, freeAt(cells, DayOfWeek.SATURDAY, 17));
    }

    @Test
    void heatmapIgnoresPartialHours() {
        // Free 18:30-20:00 covers 19:00-20:00 but not 18:00-19:00.
        List<HeatmapCell> cells = service.heatmap(List.of(
                List.of(slot(DayOfWeek.MONDAY, 18, 30, 20, 0))));

        assertEquals(0, freeAt(cells, DayOfWeek.MONDAY, 18));
        assertEquals(1, freeAt(cells, DayOfWeek.MONDAY, 19));
    }

    @Test
    void heatmapCountsAMemberOnceEvenWithTwoSlotsInTheSameHour() {
        List<HeatmapCell> cells = service.heatmap(List.of(
                List.of(slot(DayOfWeek.MONDAY, 9, 12), slot(DayOfWeek.MONDAY, 10, 11))));

        assertEquals(1, freeAt(cells, DayOfWeek.MONDAY, 10));
    }

    // ---------- suggestForGroup ----------

    @Test
    void groupPrefersWindowWithMoreMembers() {
        List<TimeSlot> m1 = List.of(slot(DayOfWeek.SATURDAY, 14, 16), slot(DayOfWeek.TUESDAY, 19, 21));
        List<TimeSlot> m2 = List.of(slot(DayOfWeek.SATURDAY, 14, 16), slot(DayOfWeek.TUESDAY, 19, 21));
        List<TimeSlot> m3 = List.of(slot(DayOfWeek.SATURDAY, 14, 16));

        List<FreeWindow> result = service.suggestForGroup(List.of(m1, m2, m3));

        assertEquals(2, result.size());
        assertEquals(slot(DayOfWeek.SATURDAY, 14, 16), result.get(0).slot());
        assertEquals(3, result.get(0).freeCount());
        assertEquals(slot(DayOfWeek.TUESDAY, 19, 21), result.get(1).slot());
        assertEquals(2, result.get(1).freeCount());
    }

    @Test
    void groupWindowMergesConsecutiveHours() {
        List<FreeWindow> result = service.suggestForGroup(List.of(
                List.of(slot(DayOfWeek.WEDNESDAY, 10, 14)),
                List.of(slot(DayOfWeek.WEDNESDAY, 10, 14))));

        assertEquals(1, result.size());
        assertEquals(slot(DayOfWeek.WEDNESDAY, 10, 14), result.get(0).slot());
    }

    @Test
    void groupWindowCanRunToTheLastHourOfTheGrid() {
        List<FreeWindow> result = service.suggestForGroup(List.of(
                List.of(slot(DayOfWeek.MONDAY, 21, 23)),
                List.of(slot(DayOfWeek.MONDAY, 21, 23))));

        assertEquals(1, result.size());
        assertEquals(slot(DayOfWeek.MONDAY, 21, 23), result.get(0).slot());
    }

    @Test
    void groupOfOneGetsNoSuggestionsButStillGetsAHeatmap() {
        List<List<TimeSlot>> solo = List.of(List.of(slot(DayOfWeek.MONDAY, 9, 11)));

        assertTrue(service.suggestForGroup(solo).isEmpty());
        assertEquals(1, freeAt(service.heatmap(solo), DayOfWeek.MONDAY, 9));
    }

    @Test
    void groupWithNoSharedTimeGetsNoSuggestions() {
        List<FreeWindow> result = service.suggestForGroup(List.of(
                List.of(slot(DayOfWeek.MONDAY, 9, 11)),
                List.of(slot(DayOfWeek.TUESDAY, 9, 11))));

        assertTrue(result.isEmpty());
    }
}