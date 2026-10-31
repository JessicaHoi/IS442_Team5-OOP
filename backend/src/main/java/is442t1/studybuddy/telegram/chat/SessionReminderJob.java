package is442t1.studybuddy.telegram.chat;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Looks for study sessions starting soon. Instances sharing a bot can all run this: each reminder is claimed first. */
@Component
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
class SessionReminderJob {

    private final SessionChatService sessionChat;

    SessionReminderJob(SessionChatService sessionChat) {
        this.sessionChat = sessionChat;
    }

    @Scheduled(fixedDelayString = "${studybuddy.telegram.reminder-check}", initialDelayString = "${studybuddy.telegram.reminder-check}")
    void run() {
        sessionChat.sendDueReminders(Instant.now());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
    static class SchedulingConfig {
    }
}
