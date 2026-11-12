package is442t1.studybuddy.telegram.api;

/**
 * An invite link returned by Telegram.
 *
 * @param inviteLink the invite URL
 * @param name the label StudyBuddy gave the link, or null for links it didn't create
 */
public record ChatInviteLink(String inviteLink, String name) {
}
