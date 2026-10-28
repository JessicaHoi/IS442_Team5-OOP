package is442t1.studybuddy.group;

import java.util.List;
import java.util.UUID;
import is442t1.studybuddy.common.web.TimeSlotDto;

public record GroupAvailabilityResponse(String source, UUID revision, List<TimeSlotDto> availability) { }
