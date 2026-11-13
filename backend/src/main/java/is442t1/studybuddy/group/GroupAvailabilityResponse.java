package is442t1.studybuddy.group;

import java.util.List;
import java.util.UUID;
import is442t1.studybuddy.common.web.TimeSlotDto;

/**
 * Effective availability for the caller in one group.
 *
 * @param source GROUP when an override exists, otherwise PROFILE for the group course
 * @param revision override revision UUID, or null when following the profile
 * @param availability effective weekly slots; an empty GROUP list means explicitly unavailable
 */
public record GroupAvailabilityResponse(String source, UUID revision, List<TimeSlotDto> availability) { }
