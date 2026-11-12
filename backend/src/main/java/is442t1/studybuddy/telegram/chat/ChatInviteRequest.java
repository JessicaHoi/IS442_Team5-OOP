package is442t1.studybuddy.telegram.chat;

/**
 * A request for an invite to a linked chat.
 *
 * @param renew whether to replace the current invite, or remove whoever joined with it ("Not you?")
 */
public record ChatInviteRequest(boolean renew) {
}
