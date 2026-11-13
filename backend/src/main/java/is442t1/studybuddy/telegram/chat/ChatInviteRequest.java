package is442t1.studybuddy.telegram.chat;

/**
 * Optional body for creating or renewing the caller's managed group-chat invite.
 *
 * @param renew replace the current invite, or remove whoever joined with it ("Not you?")
 */
public record ChatInviteRequest(boolean renew) {
}
