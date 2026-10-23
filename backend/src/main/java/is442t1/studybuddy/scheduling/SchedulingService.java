package is442t1.studybuddy.scheduling;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import is442t1.studybuddy.model.TimeSlot;

/**
 * Works out when people are free at the same time.
 */
@Service
public class SchedulingService {

    private static final Duration MIN_DURATION = Duration.ofMinutes(30);
    private static final int MAX_SUGGESTIONS = 3;
    private static final int FIRST_HOUR = 7;   // heatmap starts at 07:00
    private static final int LAST_HOUR = 23;   // last cell is 22:00-23:00

    /** Exact overlaps between two students, longest first. */
    public List<TimeSlot> suggestForPair(List<TimeSlot> a, List<TimeSlot> b) {
        return a.stream()
                .flatMap(x -> b.stream().map(x::intersect))
                .flatMap(Optional::stream)
                .filter(s -> s.duration().compareTo(MIN_DURATION) >= 0)
                .sorted(Comparator.comparing(TimeSlot::duration).reversed())
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    /** How many members are free in each whole hour, Monday to Sunday. */
    public List<HeatmapCell> heatmap(Collection<List<TimeSlot>> membersAvailability) {
        List<HeatmapCell> cells = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (int hour = FIRST_HOUR; hour < LAST_HOUR; hour++) {
                LocalTime start = LocalTime.of(hour, 0);
                LocalTime end = LocalTime.of(hour + 1, 0);
                int free = (int) membersAvailability.stream()
                        .filter(slots -> slots.stream().anyMatch(s -> covers(s, day, start, end)))
                        .count();
                cells.add(new HeatmapCell(day, hour, free));
            }
        }
        return cells;
    }

    /** Best windows for a group: runs of consecutive hours with the same head count (at least 2 people). */
    public List<FreeWindow> suggestForGroup(Collection<List<TimeSlot>> membersAvailability) {
        List<HeatmapCell> cells = heatmap(membersAvailability);
        List<FreeWindow> windows = new ArrayList<>();
        int i = 0;
        while (i < cells.size()) {
            HeatmapCell first = cells.get(i);
            int j = i + 1;
            while (j < cells.size()
                    && cells.get(j).day() == first.day()
                    && cells.get(j).freeCount() == first.freeCount()) {
                j++;
            }
            if (first.freeCount() >= 2) {
                TimeSlot slot = new TimeSlot(
                        first.day(),
                        LocalTime.of(first.hour(), 0),
                        LocalTime.of(cells.get(j - 1).hour() + 1, 0));
                windows.add(new FreeWindow(slot, first.freeCount()));
            }
            i = j;
        }
        return windows.stream()
                .sorted(Comparator.comparingInt(FreeWindow::freeCount).reversed()
                        .thenComparing(w -> w.slot().duration(), Comparator.reverseOrder()))
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    /** True when the slot covers the whole hour from start to end on that day. */
    private boolean covers(TimeSlot s, DayOfWeek day, LocalTime start, LocalTime end) {
        return s.day() == day && !s.startTime().isAfter(start) && !s.endTime().isBefore(end);
    }
}