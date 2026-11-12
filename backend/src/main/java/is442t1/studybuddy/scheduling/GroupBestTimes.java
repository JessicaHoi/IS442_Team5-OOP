package is442t1.studybuddy.scheduling;

import java.util.List;

/**
 * Suggested meeting windows and the full availability heatmap for a group.
 *
 * @param suggestions the suggested meeting windows
 * @param heatmap the availability blocks for the group
 * @param memberCount the number of group members
 */
public record GroupBestTimes(List<FreeWindow> suggestions, List<GroupHeatmapCell> heatmap, int memberCount) { }
