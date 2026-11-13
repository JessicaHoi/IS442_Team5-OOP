package is442t1.studybuddy.telegram.api;

/**
 * Telegram invite URL and label used to recognise StudyBuddy's personal invites.
 *
 * @param name the label StudyBuddy gave the link, or null for links it didn't create
 */
public record ChatInviteLink(String inviteLink, String name) {
}
