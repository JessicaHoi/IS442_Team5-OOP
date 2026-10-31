package is442t1.studybuddy.telegram.api;

import java.util.List;
import java.util.Map;

/** The buttons that can go with a message. */
public sealed interface ReplyMarkup {

    /**
     * Buttons under the message box. {@code isPersistent} stops Telegram tucking them away behind the keyboard icon,
     * and they stay until {@link RemoveKeyboard}, so a leader who backs out can try again.
     */
    record Keyboard(List<List<ChatRequestButton>> keyboard, boolean isPersistent, boolean resizeKeyboard) implements ReplyMarkup {
    }

    record RemoveKeyboard(boolean removeKeyboard) implements ReplyMarkup {
    }

    /** One button that opens a link, attached to the message. */
    record LinkButton(List<List<Url>> inlineKeyboard) implements ReplyMarkup {

        public static LinkButton of(String text, String url) {
            return new LinkButton(List.of(List.of(new Url(text, url))));
        }

        public record Url(String text, String url) {
        }
    }

    /** Buttons attached to the message that send StudyBuddy {@code callbackData} when pressed. */
    record ActionButtons(List<List<Action>> inlineKeyboard) implements ReplyMarkup {

        public static ActionButtons inARow(Action... actions) {
            return new ActionButtons(List.of(List.of(actions)));
        }

        /** @param callbackData at most 64 bytes */
        public record Action(String text, String callbackData) {
        }
    }

    record ChatRequestButton(String text, ChatRequest requestChat) {
    }

    /** @param botAdministratorRights null lists every group the user owns, and the bot then joins without admin rights */
    record ChatRequest(int requestId, boolean chatIsChannel, boolean botIsMember,
            Map<String, Boolean> userAdministratorRights, Map<String, Boolean> botAdministratorRights) {
    }
}
