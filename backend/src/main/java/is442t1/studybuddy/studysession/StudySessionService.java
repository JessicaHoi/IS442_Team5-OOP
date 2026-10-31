package is442t1.studybuddy.studysession;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.AppProperties;
import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.group.StudyGroup;
import is442t1.studybuddy.group.StudyGroupRepository;
import is442t1.studybuddy.model.enums.SessionStatus;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentService;
import is442t1.studybuddy.studysession.event.SessionAnswered;
import is442t1.studybuddy.studysession.event.SessionCancelled;
import is442t1.studybuddy.studysession.event.SessionChanged;
import is442t1.studybuddy.studysession.event.SessionScheduled;

/**
 * Scheduling study sessions, answering them and exporting them to a calendar.
 * Every change locks the group first, so it cannot race with closing the group or a member leaving.
 */
@Service
@Transactional
public class StudySessionService {

    private final StudySessionRepository sessionRepository;
    private final StudyGroupRepository groupRepository;
    private final StudentService studentService;
    private final AppProperties appProperties;
    private final ApplicationEventPublisher events;

    public StudySessionService(StudySessionRepository sessionRepository, StudyGroupRepository groupRepository,
            StudentService studentService, AppProperties appProperties, ApplicationEventPublisher events) {
        this.sessionRepository = sessionRepository;
        this.groupRepository = groupRepository;
        this.studentService = studentService;
        this.appProperties = appProperties;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<StudySessionResponse> listForGroup(UUID callerId, UUID groupId, String scope) {
        requireMember(requireGroup(groupId), callerId);
        Instant now = Instant.now();
        List<StudySession> sessions = switch (scope) {
            case "upcoming" -> sessionRepository.findByStudyGroup_IdAndEndsAtAfterOrderByStartsAtAsc(groupId, now);
            case "past" -> sessionRepository.findByStudyGroup_IdAndEndsAtLessThanEqualOrderByStartsAtDesc(groupId, now);
            default -> throw new ValidationException("Scope must be upcoming or past.");
        };
        return sessions.stream().map(session -> StudySessionResponse.from(session, callerId)).toList();
    }

    @Transactional(readOnly = true)
    public List<StudySessionResponse> listUpcomingForStudent(UUID studentId) {
        return sessionRepository.findUpcomingForStudent(studentId, Instant.now()).stream()
                .map(session -> StudySessionResponse.from(session, studentId))
                .toList();
    }

    public StudySessionResponse schedule(UUID callerId, UUID groupId, SessionDetails details) {
        StudyGroup group = requireGroupForUpdate(groupId);
        requireActive(callerId);
        StudySession session = sessionRepository.save(StudySession.schedule(group, callerId, details, Instant.now()));
        events.publishEvent(new SessionScheduled(session.getId()));
        return StudySessionResponse.from(session, callerId);
    }

    public StudySessionResponse update(UUID callerId, UUID groupId, UUID sessionId, SessionDetails details) {
        requireGroupForUpdate(groupId);
        requireActive(callerId);
        StudySession session = requireSession(groupId, sessionId);
        SessionChange change = session.update(callerId, details, Instant.now());
        if (change != SessionChange.NONE) {
            events.publishEvent(new SessionChanged(session.getId(), change == SessionChange.START_MOVED));
        }
        return StudySessionResponse.from(session, callerId);
    }

    public StudySessionResponse cancel(UUID callerId, UUID groupId, UUID sessionId) {
        requireGroupForUpdate(groupId);
        requireActive(callerId);
        StudySession session = requireSession(groupId, sessionId);
        session.cancel(callerId, Instant.now());
        events.publishEvent(new SessionCancelled(session.getId()));
        return StudySessionResponse.from(session, callerId);
    }

    public StudySessionResponse respond(UUID callerId, UUID groupId, UUID sessionId, RsvpRequest request) {
        requireGroupForUpdate(groupId);
        StudySession session = requireSession(groupId, sessionId);
        Student student = studentService.requireStudent(callerId);
        session.respond(student, request.response(), Instant.now());
        sessionRepository.flush();
        events.publishEvent(new SessionAnswered(session.getId()));
        return StudySessionResponse.from(session, callerId);
    }

    @Transactional(readOnly = true)
    public String calendarFile(UUID callerId, UUID groupId, UUID sessionId) {
        StudyGroup group = requireGroup(groupId);
        requireMember(group, callerId);
        StudySession session = requireSession(groupId, sessionId);
        String description = Stream.of("Study group: " + group.getGroupName(), howToJoin(session), session.getNote())
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
        IcsWriter.Event event = new IcsWriter.Event(session.getId() + "@studybuddy", session.getIcsSequence(),
                session.getTitle(), description, session.getLocation(), session.getStartsAt(), session.getEndsAt(),
                session.getStatus() == SessionStatus.CANCELLED, groupPageUrl(groupId));
        return IcsWriter.write(event, Instant.now());
    }

    private static String howToJoin(StudySession session) {
        if (session.getMeetingLink() != null) {
            return "Join: " + session.getMeetingLink();
        }
        return session.isGroupVideoChat() ? "Video chat in the group's Telegram chat" : null;
    }

    private String groupPageUrl(UUID groupId) {
        String base = appProperties.url();
        if (base == null || base.isBlank()) {
            return null;
        }
        return base.strip().replaceAll("/+$", "") + "/#/groups/" + groupId; // the app uses hash routing
    }

    private StudyGroup requireGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Study group not found."));
    }

    private StudyGroup requireGroupForUpdate(UUID groupId) {
        return groupRepository.findForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Study group not found."));
    }

    private StudySession requireSession(UUID groupId, UUID sessionId) {
        return sessionRepository.findByIdAndStudyGroup_Id(sessionId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Study session not found."));
    }

    private void requireActive(UUID callerId) {
        if (!studentService.requireStudent(callerId).isActive()) {
            throw new PermissionDeniedException("An active account is required.");
        }
    }

    private static void requireMember(StudyGroup group, UUID studentId) {
        if (!group.hasMember(studentId)) {
            throw new PermissionDeniedException("Only group members can see study sessions.");
        }
    }
}
