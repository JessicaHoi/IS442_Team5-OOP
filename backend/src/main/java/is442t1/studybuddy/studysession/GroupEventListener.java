package is442t1.studybuddy.studysession;

import java.time.Instant;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import is442t1.studybuddy.group.event.GroupClosed;
import is442t1.studybuddy.group.event.MemberLeft;

/** Keeps sessions in step with their group. Runs inside the transaction that changed the group. */
@Component
class GroupEventListener {

    private final StudySessionRepository sessionRepository;

    GroupEventListener(StudySessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @EventListener
    void onGroupClosed(GroupClosed event) {
        Instant now = Instant.now();
        sessionRepository.findByStudyGroup_IdAndStartsAtAfter(event.groupId(), now)
                .forEach(session -> session.cancelBecauseGroupClosed(now));
    }

    @EventListener
    void onMemberLeft(MemberLeft event) {
        sessionRepository.findByStudyGroup_IdAndStartsAtAfter(event.groupId(), Instant.now())
                .forEach(session -> session.removeRsvp(event.studentId()));
    }
}
