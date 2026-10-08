package is442t1.studybuddy.connection;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only view of connections between two students. Profiles use it to
 * decide whether the contact number may be shown.
 */
@Service
@Transactional(readOnly = true)
public class BuddyStateService {

    private final StudyBuddyConnectionRepository connectionRepository;
    private final MatchRequestRepository matchRequestRepository;

    public BuddyStateService(StudyBuddyConnectionRepository connectionRepository,
            MatchRequestRepository matchRequestRepository) {
        this.connectionRepository = connectionRepository;
        this.matchRequestRepository = matchRequestRepository;
    }

    public BuddyState stateBetween(UUID viewerId, UUID otherId) {
        if (viewerId.equals(otherId)) {
            return BuddyState.NONE;
        }
        return connectionRepository.findActiveBetween(viewerId, otherId)
                .map(connection -> new BuddyState(BuddyStatus.CONNECTED, connection.getId()))
                .or(() -> matchRequestRepository.findPendingBetween(viewerId, otherId).stream().findFirst()
                        .map(request -> new BuddyState(
                                request.getSender().hasId(viewerId) ? BuddyStatus.PENDING_SENT : BuddyStatus.PENDING_RECEIVED,
                                request.getId())))
                .orElse(BuddyState.NONE);
    }
}
