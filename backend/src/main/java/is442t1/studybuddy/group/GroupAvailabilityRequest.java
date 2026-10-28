package is442t1.studybuddy.group;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import is442t1.studybuddy.common.web.TimeSlotDto;

public record GroupAvailabilityRequest(
        UUID revision,
        @NotNull(message = "Provide your availability, or an empty list if unavailable.")
        @Size(max = 512, message = "There are too many availability slots.")
        List<@NotNull @Valid TimeSlotDto> availability
) { }
