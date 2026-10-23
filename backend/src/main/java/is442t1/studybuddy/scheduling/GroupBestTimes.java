package is442t1.studybuddy.scheduling;

import java.util.List;

/** What the group page needs: the top suggested windows plus the full heatmap grid. */
public record GroupBestTimes(List<FreeWindow> suggestions, List<HeatmapCell> heatmap) { }