package is442t1.studybuddy.telegram.bot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import is442t1.studybuddy.telegram.api.Update;

class UpdateClassifierTest {

    private static final Update.Sender PERSON = new Update.Sender(42, false, "Aisha", "aisha_r");
    private static final Update.Chat PRIVATE = new Update.Chat(42, "private");

    private static Update update(Update.Sender from, Update.Chat chat, String text) {
        return new Update(1, new Update.Message(2, from, chat, text));
    }

    @Test
    void startWithAPayloadInAPrivateChat() {
        assertThat(UpdateClassifier.classify(update(PERSON, PRIVATE, "/start a_abc")))
                .isEqualTo(new BotEvent.StartCommand(42, 42, "aisha_r", "Aisha", "a_abc"));
    }

    @Test
    void startWithoutAPayloadHasAnEmptyOne() {
        assertThat(UpdateClassifier.classify(update(PERSON, PRIVATE, "/start")))
                .isEqualTo(new BotEvent.StartCommand(42, 42, "aisha_r", "Aisha", ""));
    }

    @Test
    void aLinkTokenPostedInAGroupIsIgnored() {
        assertThat(UpdateClassifier.classify(update(PERSON, new Update.Chat(-100, "supergroup"), "/start a_abc")))
                .isInstanceOf(BotEvent.Ignored.class);
    }

    @Test
    void groupMessagesAreIgnored() {
        assertThat(UpdateClassifier.classify(update(PERSON, new Update.Chat(-100, "supergroup"), "see you at 7")))
                .isInstanceOf(BotEvent.Ignored.class);
    }

    @Test
    void otherPrivateTextIsIgnored() {
        assertThat(UpdateClassifier.classify(update(PERSON, PRIVATE, "hello"))).isInstanceOf(BotEvent.Ignored.class);
        assertThat(UpdateClassifier.classify(update(PERSON, PRIVATE, "/startle"))).isInstanceOf(BotEvent.Ignored.class);
    }

    @Test
    void otherBotsAreIgnored() {
        assertThat(UpdateClassifier.classify(update(new Update.Sender(7, true, "Bot", "other_bot"), PRIVATE, "/start a_abc")))
                .isInstanceOf(BotEvent.Ignored.class);
    }

    @Test
    void updatesWithoutUsableMessagesAreIgnored() {
        assertThat(UpdateClassifier.classify(new Update(1, null))).isInstanceOf(BotEvent.Ignored.class);
        assertThat(UpdateClassifier.classify(update(null, PRIVATE, "/start"))).isInstanceOf(BotEvent.Ignored.class);
        assertThat(UpdateClassifier.classify(update(PERSON, PRIVATE, null))).isInstanceOf(BotEvent.Ignored.class);
    }
}
