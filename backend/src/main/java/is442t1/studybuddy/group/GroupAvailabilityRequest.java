package is442t1.studybuddy.group;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import is442t1.studybuddy.common.web.TimeSlotDto;

/**
 * Replaces the caller's availability override for one group.
 *
 * @param revision null when no override exists; otherwise the UUID from the latest response, with
 *     stale values producing HTTP 409
 * @param availability non-null list of at most 512 valid, non-overlapping slots; an empty list
 *     explicitly means unavailable and does not restore profile fallback
 */
public record GroupAvailabilityRequest(
        UUID revision,
        @NotNull(message = "Provide your availability, or an empty list if unavailable.")
        @Size(max = 512, message = "There are too many availability slots.")
        List<@NotNull @Valid TimeSlotDto> availability
) { }
