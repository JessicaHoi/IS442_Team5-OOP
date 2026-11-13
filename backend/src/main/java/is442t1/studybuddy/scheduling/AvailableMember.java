package is442t1.studybuddy.scheduling;

import java.util.UUID;

/**
 * Student ID and display name associated with a group heatmap cell. Member identity is retained
 *     when merging suggestion windows.
 */
public record AvailableMember(UUID id, String name) { }
