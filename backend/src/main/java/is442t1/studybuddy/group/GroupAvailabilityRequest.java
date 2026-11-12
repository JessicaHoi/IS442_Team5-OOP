package is442t1.studybuddy.group;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import is442t1.studybuddy.common.web.TimeSlotDto;

/**
 * A member's availability update for a group.
 *
 * @param revision the revision being replaced, or {@code null} if there is no group-specific override
 * @param availability the member's time slots, or an empty list if unavailable
 */
public record GroupAvailabilityRequest(
        UUID revision,
        @NotNull(message = "Provide your availability, or an empty list if unavailable.")
        @Size(max = 512, message = "There are too many availability slots.")
        List<@NotNull @Valid TimeSlotDto> availability
) { }
