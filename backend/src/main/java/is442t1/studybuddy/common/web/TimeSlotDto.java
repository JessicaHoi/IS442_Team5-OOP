package is442t1.studybuddy.common.web;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import is442t1.studybuddy.model.TimeSlot;

/**
 * Availability slot as the API sends it: {@code {"day": "WED", "start": "19:00", "end": "21:00"}}.
 *
 * @param day the three-letter weekday code
 * @param start the start time in 24-hour format
 * @param end the end time in 24-hour format
 */
public record TimeSlotDto(
        @NotNull(message = "Choose a day for each time slot.")
        @Pattern(regexp = "MON|TUE|WED|THU|FRI|SAT|SUN", message = "Choose a valid day for each time slot.")
        String day,

        @NotNull(message = "Choose a start time for each time slot.")
        @Pattern(regexp = TIME_PATTERN, message = "Times must use the 24-hour HH:mm format.")
        String start,

        @NotNull(message = "Choose an end time for each time slot.")
        @Pattern(regexp = TIME_PATTERN, message = "Times must use the 24-hour HH:mm format.")
        String end
) {

    private static final String TIME_PATTERN = "([01]\\d|2[0-3]):[0-5]\\d";
    private static final int DAY_CODE_LENGTH = 3;

    private static final Comparator<TimeSlot> WEEK_ORDER =
            Comparator.comparing(TimeSlot::day).thenComparing(TimeSlot::startTime);

    /**
     * Converts this DTO to a domain time slot.
     *
     * @return the corresponding time slot
     * @throws IllegalArgumentException if the day code is unknown
     */
    public TimeSlot toTimeSlot() {
        return new TimeSlot(toDayOfWeek(day), LocalTime.parse(start), LocalTime.parse(end));
    }

    /**
     * Converts a domain time slot to its API representation.
     *
     * @param slot the domain time slot
     * @return the corresponding DTO
     */
    public static TimeSlotDto from(TimeSlot slot) {
        return new TimeSlotDto(
                slot.day().name().substring(0, DAY_CODE_LENGTH),
                slot.startTime().toString(),
                slot.endTime().toString());
    }

    /**
     * Converts API time slots to domain time slots in the same order.
     *
     * @param slots the API time slots
     * @return the corresponding domain time slots
     */
    public static List<TimeSlot> toTimeSlots(List<TimeSlotDto> slots) {
        return slots.stream().map(TimeSlotDto::toTimeSlot).toList();
    }

    /**
     * Converts slots for a response, sorted Monday first, then by start time.
     *
     * @param slots the domain time slots
     * @return the sorted API time slots
     */
    public static List<TimeSlotDto> fromTimeSlots(List<TimeSlot> slots) {
        return slots.stream().sorted(WEEK_ORDER).map(TimeSlotDto::from).toList();
    }

    private static DayOfWeek toDayOfWeek(String code) {
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day.name().startsWith(code)) {
                return day;
            }
        }
        throw new IllegalArgumentException("Unknown day code: " + code);
    }
}
