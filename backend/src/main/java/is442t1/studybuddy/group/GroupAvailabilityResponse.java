package is442t1.studybuddy.group;

import java.util.List;
import java.util.UUID;
import is442t1.studybuddy.common.web.TimeSlotDto;

/**
 * A member's availability for a group.
 *
 * @param source either {@code GROUP} for a group-specific override or {@code PROFILE} for profile availability
 * @param revision the group-specific override revision, or {@code null} when the profile availability is used
 * @param availability the available time slots
 */
public record GroupAvailabilityResponse(String source, UUID revision, List<TimeSlotDto> availability) { }
