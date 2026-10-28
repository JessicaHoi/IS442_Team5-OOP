package is442t1.studybuddy.scheduling;

import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.connection.StudyBuddyConnection;
import is442t1.studybuddy.connection.StudyBuddyConnectionRepository;
import is442t1.studybuddy.group.StudyGroup;
import is442t1.studybuddy.group.StudyGroupRepository;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.student.Student;

/**
 * Loads a buddy connection or study group, checks that the caller belongs
 * to it, and asks {@link SchedulingService} when everyone is free.
 */
@Service
@Transactional(readOnly = true)
public class BestTimeService {

    private final StudyBuddyConnectionRepository connectionRepository;
    private final StudyGroupRepository groupRepository;
    private final SchedulingService schedulingService;

    public BestTimeService(StudyBuddyConnectionRepository connectionRepository,
                           StudyGroupRepository groupRepository,
                           SchedulingService schedulingService) {
        this.connectionRepository = connectionRepository;
        this.groupRepository = groupRepository;
        this.schedulingService = schedulingService;
    }
    private static final int MAX_SUGGESTIONS = 3;

    public List<TimeSlot> forConnection(UUID connectionId, UUID callerId) {
        StudyBuddyConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found."));

        Student requester = connection.getRequester();
        Student recipient = connection.getRecipient();

        if (!requester.hasId(callerId) && !recipient.hasId(callerId)) {
            throw new ResourceNotFoundException("Connection not found.");
        }

        // Courses where both students have set a study preference.
        return requester.getStudyPreferences().stream()
                .map(preference -> preference.getCourse().getCourseCode())
                .filter(code -> recipient.findPreferenceFor(code).isPresent())
                .flatMap(code -> schedulingService
                        .suggestForPair(requester.availabilityFor(code), recipient.availabilityFor(code))
                        .stream())
                .distinct()
                .sorted(Comparator.comparing(TimeSlot::duration).reversed())
                .limit(MAX_SUGGESTIONS)
                .toList();
    }
    public GroupBestTimes forGroup(UUID groupId, UUID callerId) {
        StudyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found."));

        if (!group.hasMember(callerId)) {
            throw new ResourceNotFoundException("Group not found.");
        }

        Map<AvailableMember, List<TimeSlot>> everyone = new LinkedHashMap<>();
        group.getMembers().forEach(member -> everyone.put(new AvailableMember(member.getId(), member.getName()),
                group.memberAvailability(member.getId())));
        List<GroupHeatmapCell> cells = schedulingService.groupHeatmap(everyone);
        return new GroupBestTimes(schedulingService.groupSuggestions(cells), cells, everyone.size());
    }
}
