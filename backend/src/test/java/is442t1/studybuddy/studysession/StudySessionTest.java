package is442t1.studybuddy.studysession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.group.GroupDetails;
import is442t1.studybuddy.group.StudyGroup;
import is442t1.studybuddy.group.StudyGroupLeader;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.model.enums.RsvpResponse;
import is442t1.studybuddy.model.enums.SessionStatus;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;
import is442t1.studybuddy.student.Student;

/** Domain rules for study sessions, tested without Spring or a database. */
class StudySessionTest {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant START = NOW.plus(Duration.ofDays(3));

    private StudyGroupLeader leader;
    private Student member;
    private StudyGroup group;

    @BeforeEach
    void setUp() {
        leader = new StudyGroupLeader();
        leader.setId(UUID.randomUUID());
        leader.setName("Leader");
        group = leader.createGroup(new Course(), new GroupDetails("Group", "About", Set.of(StudyGoal.CONCEPT_REVIEW),
                StudyMode.IN_PERSON, List.of(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0))), 5));
        member = student("Member");
        leader.acceptMember(group.requestToJoin(member, null));
    }

    @Test
    void theLeaderSchedulesASession() {
        StudySession session = schedule(valid());

        assertThat(session.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
        assertThat(session.getIcsSequence()).isZero();
        assertThat(session.getTitle()).isEqualTo("Paper drill");
    }

    @Test
    void aNonLeaderIsRefusedBeforeTheValuesAreChecked() {
        SessionDetails invalid = new SessionDetails(" ", null, null, null, null, null);

        assertThatThrownBy(() -> StudySession.schedule(group, member.getId(), invalid, NOW))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void refusesInvalidDetails() {
        assertInvalid(new SessionDetails(" ", START, START.plus(Duration.ofHours(1)), StudyMode.ONLINE, null, null));
        assertInvalid(new SessionDetails("x".repeat(81), START, START.plus(Duration.ofHours(1)), StudyMode.ONLINE, null, null));
        assertInvalid(new SessionDetails("Title", null, START, StudyMode.ONLINE, null, null));
        assertInvalid(new SessionDetails("Title", START, START, StudyMode.ONLINE, null, null));
        assertInvalid(new SessionDetails("Title", START, START.plus(Duration.ofHours(8)).plusSeconds(1), StudyMode.ONLINE, null, null));
        assertInvalid(new SessionDetails("Title", NOW, NOW.plus(Duration.ofHours(1)), StudyMode.ONLINE, null, null));
        assertInvalid(new SessionDetails("Title", START, START.plus(Duration.ofHours(1)), StudyMode.EITHER, null, null));
        assertInvalid(new SessionDetails("Title", START, START.plus(Duration.ofHours(1)), null, null, null));
        assertInvalid(new SessionDetails("Title", START, START.plus(Duration.ofHours(1)), StudyMode.ONLINE, "x".repeat(201), null));
        assertInvalid(new SessionDetails("Title", START, START.plus(Duration.ofHours(1)), StudyMode.ONLINE, null, "x".repeat(501)));
    }

    @Test
    void anEightHourSessionIsAllowed() {
        schedule(new SessionDetails("Title", START, START.plus(Duration.ofHours(8)), StudyMode.ONLINE, null, null));
    }

    @Test
    void aClosedGroupCannotScheduleSessions() {
        leader.closeGroup(group);

        assertThatThrownBy(() -> schedule(valid())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updatingAndCancellingBumpTheCalendarSequence() {
        StudySession session = schedule(valid());

        session.update(leader.getId(), valid(), NOW);
        assertThat(session.getIcsSequence()).isEqualTo(1);

        session.cancel(leader.getId(), NOW);
        assertThat(session.getIcsSequence()).isEqualTo(2);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.CANCELLED);
    }

    @Test
    void aCancelledOrStartedSessionCannotChange() {
        StudySession cancelled = schedule(valid());
        cancelled.cancel(leader.getId(), NOW);
        StudySession started = schedule(valid());

        assertThatThrownBy(() -> cancelled.cancel(leader.getId(), NOW)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> cancelled.respond(member, RsvpResponse.GOING, NOW)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> started.update(leader.getId(), valid(), START)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> started.respond(member, RsvpResponse.GOING, START)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void aMemberCanChangeTheirAnswerWithoutGainingASecondOne() {
        StudySession session = schedule(valid());

        session.respond(member, RsvpResponse.GOING, NOW);
        session.respond(member, RsvpResponse.NOT_GOING, NOW);

        assertThat(session.getRsvps()).hasSize(1);
        assertThat(session.findRsvp(member.getId()).orElseThrow().getResponse()).isEqualTo(RsvpResponse.NOT_GOING);
    }

    @Test
    void onlyActiveMembersCanRespond() {
        StudySession session = schedule(valid());
        Student stranger = student("Stranger");
        member.setStatus(AccountStatus.SUSPENDED);

        assertThatThrownBy(() -> session.respond(stranger, RsvpResponse.GOING, NOW)).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> session.respond(member, RsvpResponse.GOING, NOW)).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void closingTheGroupOnlyCancelsSessionsThatHaveNotStarted() {
        StudySession upcoming = schedule(valid());
        StudySession started = schedule(valid());

        upcoming.cancelBecauseGroupClosed(NOW);
        started.cancelBecauseGroupClosed(START);

        assertThat(upcoming.getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(started.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
    }

    private StudySession schedule(SessionDetails details) {
        return StudySession.schedule(group, leader.getId(), details, NOW);
    }

    private void assertInvalid(SessionDetails details) {
        assertThatThrownBy(() -> schedule(details)).isInstanceOf(ValidationException.class);
    }

    private static SessionDetails valid() {
        return new SessionDetails(" Paper drill ", START, START.plus(Duration.ofHours(2)), StudyMode.IN_PERSON, "SCIS GSR 2-1", "Bring notes");
    }

    private static Student student(String name) {
        Student student = new Student();
        student.setId(UUID.randomUUID());
        student.setName(name);
        return student;
    }
}
