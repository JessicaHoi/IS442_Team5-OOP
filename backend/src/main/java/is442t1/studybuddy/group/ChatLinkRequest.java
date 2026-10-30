package is442t1.studybuddy.group;

/** Body of {@code PUT /api/groups/{id}/chat-link}. Checked after leadership, so a non-leader always gets 403. */
public record ChatLinkRequest(String url) {
}
