package is442t1.studybuddy.telegram.api;

import java.util.List;
import java.util.Map;

/**
 * The buttons or keyboard behavior that can accompany a Telegram message.
 */
public sealed interface ReplyMarkup {

    /**
     * Buttons under the message box. {@code isPersistent} stops Telegram tucking them away behind the keyboard icon,
     * and they stay until {@link RemoveKeyboard}, so a leader who backs out can try again.
     *
     * @param keyboard rows of chat-request buttons shown under the message box
     * @param isPersistent whether Telegram keeps the keyboard visible
     * @param resizeKeyboard whether Telegram may resize the keyboard to fit its buttons
     */
    record Keyboard(List<List<ChatRequestButton>> keyboard, boolean isPersistent, boolean resizeKeyboard) implements ReplyMarkup {
    }

    /**
     * Instructs Telegram to remove the custom keyboard.
     *
     * @param removeKeyboard whether to remove the current custom keyboard
     */
    record RemoveKeyboard(boolean removeKeyboard) implements ReplyMarkup {
    }

    /**
     * Inline buttons that open URLs when pressed.
     *
     * @param inlineKeyboard rows of URL buttons
     */
    record LinkButton(List<List<Url>> inlineKeyboard) implements ReplyMarkup {

        /**
         * Creates a one-row keyboard containing one URL button.
         *
         * @param text the button label
         * @param url the URL to open
         * @return the inline keyboard
         */
        public static LinkButton of(String text, String url) {
            return new LinkButton(List.of(List.of(new Url(text, url))));
        }

        /**
         * An inline button that opens a URL.
         *
         * @param text the button label
         * @param url the URL to open
         */
        public record Url(String text, String url) {
        }
    }

    /**
     * Inline buttons that send StudyBuddy {@code callbackData} when pressed.
     *
     * @param inlineKeyboard rows of action buttons
     */
    record ActionButtons(List<List<Action>> inlineKeyboard) implements ReplyMarkup {

        /**
         * Creates one row containing the supplied action buttons.
         *
         * @param actions the buttons to place in the row
         * @return the inline keyboard
         */
        public static ActionButtons inARow(Action... actions) {
            return new ActionButtons(List.of(List.of(actions)));
        }

        /**
         * An inline action button handled by the bot.
         *
         * @param text the button label
         * @param callbackData the data sent back when pressed, at most 64 bytes
         */
        public record Action(String text, String callbackData) {
        }
    }

    /**
     * A button that asks Telegram's client to let the user choose a chat.
     *
     * @param text the button label
     * @param requestChat the chat-selection settings
     */
    record ChatRequestButton(String text, ChatRequest requestChat) {
    }

    /**
     * Settings for a Telegram chat-request button.
     *
     * @param requestId the identifier included with the selected chat
     * @param chatIsChannel whether the button requests a channel rather than a group
     * @param botIsMember whether the bot should be a member of the selected chat
     * @param userAdministratorRights the administrator rights required of the selecting user, or null for no restriction
     * @param botAdministratorRights the administrator rights requested for the bot, or null to join without admin rights
     */
    record ChatRequest(int requestId, boolean chatIsChannel, boolean botIsMember,
            Map<String, Boolean> userAdministratorRights, Map<String, Boolean> botAdministratorRights) {
    }
}
