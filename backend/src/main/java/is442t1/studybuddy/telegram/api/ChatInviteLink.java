package is442t1.studybuddy.telegram.api;

/** @param name the label StudyBuddy gave the link, or null for links it didn't create */
public record ChatInviteLink(String inviteLink, String name) {
}
