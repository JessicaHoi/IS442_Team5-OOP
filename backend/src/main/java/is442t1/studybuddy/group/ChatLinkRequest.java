package is442t1.studybuddy.group;

/**
 * Body of {@code PUT /api/groups/{id}/chat-link}. For an existing group,
 * leadership is checked before the URL is validated.
 *
 * @param url the group chat link
 */
public record ChatLinkRequest(String url) {
}
