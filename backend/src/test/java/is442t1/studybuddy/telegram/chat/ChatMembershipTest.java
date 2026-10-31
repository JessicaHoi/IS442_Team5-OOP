package is442t1.studybuddy.telegram.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatMembershipTest {

    private static final Instant NOW = Instant.parse("2026-10-31T00:00:00Z");
    private static final long ASKING = 42;

    private ChatMembership membership;

    @BeforeEach
    void setUp() {
        membership = new ChatMembership(null, null, NOW);
        membership.invite("https://t.me/+abc", "inv_1", NOW.plusSeconds(60), NOW);
    }

    private JoinDecision decide(boolean active, Long linked, boolean usedByAnother) {
        return membership.decideJoin(active, linked, usedByAnother, ASKING, NOW);
    }

    @Test
    void approvesAnActiveMemberWithAValidInvite() {
        assertThat(decide(true, null, false)).isInstanceOf(JoinDecision.Approve.class);
    }

    @Test
    void approvesWhenTheConnectedAccountIsTheOneAsking() {
        assertThat(decide(true, ASKING, false)).isInstanceOf(JoinDecision.Approve.class);
    }

    @Test
    void declinesSomeoneNoLongerInTheGroup() {
        assertThat(decide(false, null, false)).isEqualTo(new JoinDecision.Decline(ChatMembership.NOT_MEMBER));
    }

    @Test
    void declinesAnExpiredInvite() {
        assertThat(membership.decideJoin(true, null, false, ASKING, NOW.plusSeconds(61)))
                .isEqualTo(new JoinDecision.Decline(ChatMembership.LINK_NOT_VALID));
    }

    @Test
    void declinesAnInviteAlreadyUsed() {
        membership.markJoined(7, "someone", NOW);

        assertThat(decide(true, null, false)).isEqualTo(new JoinDecision.Decline(ChatMembership.LINK_NOT_VALID));
    }

    @Test
    void declinesAForwardedInviteWhenTheStudentConnectedAnotherAccount() {
        assertThat(decide(true, 99L, false)).isEqualTo(new JoinDecision.Decline(ChatMembership.WRONG_ACCOUNT));
    }

    @Test
    void declinesSomeoneAlreadyInTheChatAsAnotherStudent() {
        assertThat(decide(true, null, true)).isEqualTo(new JoinDecision.Decline(ChatMembership.ALREADY_USED_BY_ANOTHER));
    }

    @Test
    void joiningUsesUpTheInvite() {
        membership.markJoined(ASKING, "@aisha", NOW);

        assertThat(membership.getState()).isEqualTo(ChatMemberState.JOINED);
        assertThat(membership.getInviteLink()).isNull();
        assertThat(membership.getTelegramUserId()).isEqualTo(ASKING);
    }

    @Test
    void resetLetsARemovedStudentStartAgain() {
        membership.markJoined(ASKING, "@aisha", NOW);
        membership.markRemoved(NOW);

        membership.reset(NOW);

        assertThat(membership.getState()).isEqualTo(ChatMemberState.NOT_INVITED);
        assertThat(membership.getTelegramUserId()).isNull();
    }
}
