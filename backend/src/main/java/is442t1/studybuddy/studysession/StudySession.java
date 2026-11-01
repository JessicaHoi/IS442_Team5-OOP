package is442t1.studybuddy.studysession;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.group.StudyGroup;
import is442t1.studybuddy.model.BaseEntity;
import is442t1.studybuddy.model.enums.RsvpResponse;
import is442t1.studybuddy.model.enums.SessionStatus;
import is442t1.studybuddy.model.enums.StudyMode;
import is442t1.studybuddy.student.Student;

/**
 * A meeting a study group's leader schedules. The rules live here; the service
 * loads, authorises and saves.
 */
@Entity
@Table(name = "study_session")
public class StudySession extends BaseEntity {

    private static final Duration MAX_LENGTH = Duration.ofHours(8);
    /** How long after the end a leader can still cancel. The frontend has the same limit in utils/sessions.js. */
    private static final Duration CANCEL_BUFFER = Duration.ofHours(1);
    private static final int MAX_TITLE = 80;
    private static final int MAX_LOCATION = 200;
    private static final int MAX_NOTE = 500;
    private static final int MAX_MEETING_LINK = 500;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @Column(nullable = false, length = MAX_TITLE)
    private String title;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyMode meetingMode;

    @Column(length = MAX_LOCATION)
    private String location;

    @Column(length = MAX_NOTE)
    private String note;

    @Column(length = MAX_MEETING_LINK)
    private String meetingLink;

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean groupVideoChat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.SCHEDULED;

    /** The calendar SEQUENCE: it goes up with every change, so calendar apps replace their earlier copy. */
    @Column(nullable = false)
    private int icsSequence;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionRsvp> rsvps = new ArrayList<>();

    protected StudySession() {
    }

    /** Checks leadership before the new values, so a non-leader gets 403 whatever they send. */
    public static StudySession schedule(StudyGroup group, UUID callerId, SessionDetails details, Instant now) {
        requireLeader(group, callerId);
        if (!group.isOpen()) {
            throw new BusinessRuleException("This group is closed.");
        }
        StudySession session = new StudySession();
        session.studyGroup = group;
        session.apply(details, now);
        return session;
    }

    /** Says what changed. Saving without changes touches nothing, and moving the start clears answers given for the old time. */
    public SessionChange update(UUID callerId, SessionDetails details, Instant now) {
        requireLeader(studyGroup, callerId);
        requireChangeable(now);
        Instant oldStart = startsAt;
        Instant oldUpdatedAt = updatedAt;
        List<Object> before = detailValues();
        apply(details, now);
        if (before.equals(detailValues())) {
            updatedAt = oldUpdatedAt;
            return SessionChange.NONE;
        }
        icsSequence++;
        if (startsAt.equals(oldStart)) {
            return SessionChange.DETAILS;
        }
        rsvps.clear();
        return SessionChange.START_MOVED;
    }

    private List<Object> detailValues() {
        return Arrays.asList(title, startsAt, endsAt, meetingMode, location, note, meetingLink, groupVideoChat);
    }

    /** Allowed for longer than changing a session, because a meeting can fall through after its start. Later than that, the record stands. */
    public void cancel(UUID callerId, Instant now) {
        requireLeader(studyGroup, callerId);
        requireNotCancelled();
        if (now.isAfter(endsAt.plus(CANCEL_BUFFER))) {
            throw new BusinessRuleException("It's too late to cancel this session.");
        }
        markCancelled();
    }

    /** The group closed: a session that has not started will not happen. */
    void cancelBecauseGroupClosed(Instant now) {
        if (status == SessionStatus.SCHEDULED && now.isBefore(startsAt)) {
            markCancelled();
        }
    }

    public void respond(Student student, RsvpResponse response, Instant now) {
        if (!studyGroup.hasMember(student.getId())) {
            throw new PermissionDeniedException("Only group members can respond.");
        }
        if (!student.isActive()) {
            throw new PermissionDeniedException("An active account is required.");
        }
        requireChangeable(now);
        if (response == null) {
            throw new ValidationException("Choose GOING or NOT_GOING.");
        }
        Optional<SessionRsvp> existing = findRsvp(student.getId());
        if (existing.isPresent()) {
            existing.get().change(response, now);
        } else {
            rsvps.add(new SessionRsvp(this, student, response, now));
        }
    }

    /** A student who left the group no longer counts towards this session. */
    void removeRsvp(UUID studentId) {
        rsvps.removeIf(rsvp -> rsvp.getStudent().hasId(studentId));
    }

    public StudyGroup getStudyGroup() {
        return studyGroup;
    }

    public String getTitle() {
        return title;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public StudyMode getMeetingMode() {
        return meetingMode;
    }

    public String getLocation() {
        return location;
    }

    public String getNote() {
        return note;
    }

    public long goingCount() {
        return rsvps.stream().filter(rsvp -> rsvp.getResponse() == RsvpResponse.GOING).count();
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public boolean isGroupVideoChat() {
        return groupVideoChat;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public int getIcsSequence() {
        return icsSequence;
    }

    public List<SessionRsvp> getRsvps() {
        return Collections.unmodifiableList(rsvps);
    }

    public Optional<SessionRsvp> findRsvp(UUID studentId) {
        return rsvps.stream().filter(rsvp -> rsvp.getStudent().hasId(studentId)).findFirst();
    }

    private void markCancelled() {
        status = SessionStatus.CANCELLED;
        icsSequence++;
    }

    private void requireNotCancelled() {
        if (status == SessionStatus.CANCELLED) {
            throw new BusinessRuleException("This session has been cancelled.");
        }
    }

    private void requireChangeable(Instant now) {
        requireNotCancelled();
        if (!now.isBefore(startsAt)) {
            throw new BusinessRuleException("This session has already started.");
        }
    }

    private void apply(SessionDetails details, Instant now) {
        String newTitle = trimToNull(details.title());
        if (newTitle == null) {
            throw new ValidationException("Give the session a title.");
        }
        if (newTitle.length() > MAX_TITLE) {
            throw new ValidationException("Title must be at most " + MAX_TITLE + " characters.");
        }
        if (details.startsAt() == null || details.endsAt() == null) {
            throw new ValidationException("Choose a start and end time.");
        }
        if (!details.endsAt().isAfter(details.startsAt())) {
            throw new ValidationException("The session must end after it starts.");
        }
        if (Duration.between(details.startsAt(), details.endsAt()).compareTo(MAX_LENGTH) > 0) {
            throw new ValidationException("A session can be at most 8 hours long.");
        }
        if (!details.startsAt().isAfter(now)) {
            throw new ValidationException("Choose a start time in the future.");
        }
        if (details.meetingMode() != StudyMode.IN_PERSON && details.meetingMode() != StudyMode.ONLINE) {
            throw new ValidationException("Choose in-person or online.");
        }
        String newLocation = trimToNull(details.location());
        if (newLocation != null && newLocation.length() > MAX_LOCATION) {
            throw new ValidationException("Location must be at most " + MAX_LOCATION + " characters.");
        }
        String newNote = trimToNull(details.note());
        if (newNote != null && newNote.length() > MAX_NOTE) {
            throw new ValidationException("Note must be at most " + MAX_NOTE + " characters.");
        }
        String newLink = trimToNull(details.meetingLink());
        requireValidOnlineOption(details.meetingMode(), newLink, details.groupVideoChat());
        title = newTitle;
        startsAt = details.startsAt();
        endsAt = details.endsAt();
        meetingMode = details.meetingMode();
        location = newLocation;
        note = newNote;
        meetingLink = newLink;
        groupVideoChat = details.groupVideoChat();
    }

    private static void requireValidOnlineOption(StudyMode mode, String link, boolean videoChat) {
        if (link == null && !videoChat) {
            return;
        }
        if (mode != StudyMode.ONLINE) {
            throw new ValidationException("Only online sessions can have a meeting link or video chat.");
        }
        if (link != null && videoChat) {
            throw new ValidationException("Choose a meeting link or the group's video chat, not both.");
        }
        if (link != null && (link.length() > MAX_MEETING_LINK || !isWebAddress(link))) {
            throw new ValidationException("Enter a full meeting link starting with https://.");
        }
    }

    /** The link becomes a button members press, so only real web addresses get through. */
    private static boolean isWebAddress(String link) {
        try {
            URI uri = new URI(link);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static void requireLeader(StudyGroup group, UUID callerId) {
        if (!group.isLedBy(callerId)) {
            throw new PermissionDeniedException("Only the group leader can do that.");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
