package is442t1.studybuddy.scheduling;

import java.util.UUID;

/**
 * A group member available during a time block.
 *
 * @param id the member's student identifier
 * @param name the member's name
 */
public record AvailableMember(UUID id, String name) { }
