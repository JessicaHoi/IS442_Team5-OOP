package is442t1.studybuddy.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import is442t1.studybuddy.model.TimeSlot;

class GroupHeatmapTest {
    private final SchedulingService service = new SchedulingService();

    @Test
    void adjacentBlocksWithDifferentPeopleAreNotMergedIntoAnIncorrectLongerSuggestion() {
        var a = new AvailableMember(UUID.randomUUID(), "A");
        var b = new AvailableMember(UUID.randomUUID(), "B");
        var c = new AvailableMember(UUID.randomUUID(), "C");
        var cells = service.groupHeatmap(Map.of(a, List.of(slot("14:00", "15:00")),
                b, List.of(slot("14:00", "16:00")), c, List.of(slot("15:00", "16:00"))));
        var suggestions = service.groupSuggestions(cells);
        assertThat(suggestions).containsExactly(new FreeWindow(slot("14:00", "15:00"), 2), new FreeWindow(slot("15:00", "16:00"), 2));
        assertThat(cell(cells, "14:30").availableMembers()).containsExactly(a, b);
        assertThat(cell(cells, "15:00").availableMembers()).containsExactly(b, c);
    }

    @Test
    void countsOnlyFullBlocksButCombinesAdjacentExactTimeSlotsWithoutRounding() {
        var member = new AvailableMember(UUID.randomUUID(), "Member");
        var cells = service.groupHeatmap(Map.of(member, List.of(slot("14:15", "14:45"), slot("14:45", "15:00"))));
        assertThat(cell(cells, "14:00").freeCount()).isZero();
        assertThat(cell(cells, "14:30").availableMembers()).containsExactly(member);
        assertThat(cell(cells, "15:00").freeCount()).isZero();
        assertThat(service.groupSuggestions(cells)).isEmpty();
    }

    @Test
    void coversSevenDaysFromSevenToTwentyThreeWithHalfHourBlocks() {
        var cells = service.groupHeatmap(Map.of());
        assertThat(cells).hasSize(224);
        assertThat(cells.getFirst().startTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(cells.getLast().endTime()).isEqualTo(LocalTime.of(23, 0));
        assertThat(cells).allMatch(cell -> cell.freeCount() == 0 && cell.availableMembers().isEmpty());
    }

    private GroupHeatmapCell cell(List<GroupHeatmapCell> cells, String start) {
        return cells.stream().filter(cell -> cell.day() == DayOfWeek.TUESDAY && cell.startTime().equals(LocalTime.parse(start))).findFirst().orElseThrow();
    }

    private TimeSlot slot(String start, String end) { return new TimeSlot(DayOfWeek.TUESDAY, LocalTime.parse(start), LocalTime.parse(end)); }
}
