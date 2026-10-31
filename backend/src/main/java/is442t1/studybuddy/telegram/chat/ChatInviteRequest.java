package is442t1.studybuddy.telegram.chat;

/** @param renew replace the current invite, or remove whoever joined with it ("Not you?") */
public record ChatInviteRequest(boolean renew) {
}
