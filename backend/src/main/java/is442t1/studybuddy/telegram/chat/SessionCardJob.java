package is442t1.studybuddy.telegram.chat;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * How the bot does things on time: posts missing cards, moves cards on as sessions start and end, and sends reminders and nudges.
 * Only the instance reading the bot's updates runs this, so nothing is sent twice.
 */
@Component
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = {"enabled", "polling"}, havingValue = "true")
class SessionCardJob {

    private final SessionChatService sessionChat;

    SessionCardJob(SessionChatService sessionChat) {
        this.sessionChat = sessionChat;
    }

    @Scheduled(fixedDelayString = "${studybuddy.telegram.card-check}", initialDelayString = "${studybuddy.telegram.card-check}")
    void run() {
        sessionChat.sweep(Instant.now());
    }
}
