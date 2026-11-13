package is442t1.studybuddy.telegram.chat;

/** What to do about someone asking to join through an invite link. */
public sealed interface JoinDecision {

    /**
     * Decision to approve the personal-invite join request.
     */
    record Approve() implements JoinDecision {
    }

    /**
     * Decision to decline a join request and send a private explanation.
     *
     * @param reason sent to the person privately
     */
    record Decline(String reason) implements JoinDecision {
    }

    /** The link isn't StudyBuddy's, so it's left for the chat's human admins. */
    record Ignore() implements JoinDecision {
    }
}
