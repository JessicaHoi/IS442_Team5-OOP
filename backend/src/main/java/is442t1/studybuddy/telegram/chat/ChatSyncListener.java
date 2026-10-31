package is442t1.studybuddy.telegram.chat;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import is442t1.studybuddy.admin.AccountDeleting;
import is442t1.studybuddy.group.event.GroupClosed;
import is442t1.studybuddy.group.event.GroupRenamed;
import is442t1.studybuddy.group.event.LeadershipTransferred;
import is442t1.studybuddy.group.event.MemberJoined;
import is442t1.studybuddy.group.event.MemberLeft;
import is442t1.studybuddy.student.StudentRepository;
import is442t1.studybuddy.studysession.event.SessionAnswered;
import is442t1.studybuddy.studysession.event.SessionCancelled;
import is442t1.studybuddy.studysession.event.SessionChanged;
import is442t1.studybuddy.studysession.event.SessionScheduled;
import is442t1.studybuddy.telegram.chat.ChatStore.Removal;
import is442t1.studybuddy.telegram.link.TelegramAccountRepository;
import is442t1.studybuddy.telegram.TelegramProperties;

/**
 * Follows changes in study groups once they've committed, on a background thread, so a Telegram outage can't undo or
 * slow a change in StudyBuddy. Events can overtake each other, so handlers read the current state rather than trusting
 * what an event says happened.
 */
@Component
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
class ChatSyncListener {

    private static final Logger LOG = LoggerFactory.getLogger(ChatSyncListener.class);

    /** Carries what was read before an account was deleted over to after the commit. */
    record AccountDeleted(List<Removal> removals) {
    }

    private final GroupChatService chatService;
    private final SessionChatService sessionChat;
    private final SessionCardRefresher refresher;
    private final ChatStore store;
    private final StudentRepository studentRepository;
    private final TelegramAccountRepository accountRepository;
    private final TelegramProperties properties;
    private final ApplicationEventPublisher events;

    ChatSyncListener(GroupChatService chatService, SessionChatService sessionChat, SessionCardRefresher refresher, ChatStore store, StudentRepository studentRepository,
            TelegramAccountRepository accountRepository, TelegramProperties properties, ApplicationEventPublisher events) {
        this.chatService = chatService;
        this.sessionChat = sessionChat;
        this.refresher = refresher;
        this.store = store;
        this.studentRepository = studentRepository;
        this.accountRepository = accountRepository;
        this.properties = properties;
        this.events = events;
    }

    @TransactionalEventListener
    @Async("telegramExecutor")
    void on(MemberJoined event) {
        logFailure("a new member", () -> {
            sessionChat.refreshUpcoming(event.groupId());
            chatService.onMemberJoined(event.groupId(), event.studentId(), messageableUser(event.studentId()));
        });
    }

    private Long messageableUser(UUID studentId) {
        return accountRepository.findByBotIdAndStudent_Id(properties.botId(), studentId)
                .filter(account -> account.canMessage())
                .map(account -> account.getTelegramUserId())
                .orElse(null);
    }

    @TransactionalEventListener
    @Async("telegramExecutor")
    void on(MemberLeft event) {
        logFailure("a departing member", () -> {
            sessionChat.refreshUpcoming(event.groupId());
            chatService.onMemberLeft(event.groupId(), event.studentId());
        });
    }

    @TransactionalEventListener
    @Async("telegramExecutor")
    void on(GroupClosed event) {
        logFailure("a closed group", () -> {
            sessionChat.refreshUpcoming(event.groupId());
            chatService.onGroupClosed(event.groupId());
        });
    }

    @TransactionalEventListener
    @Async("telegramExecutor")
    void on(GroupRenamed event) {
        logFailure("a renamed group", () -> chatService.onGroupRenamed(event.groupId()));
    }

    @TransactionalEventListener
    @Async("telegramExecutor")
    void on(LeadershipTransferred event) {
        logFailure("a new leader", () -> studentRepository.findById(event.newLeaderId())
                .ifPresent(leader -> chatService.onLeadershipTransferred(event.groupId(), leader.getName())));
    }

    /** Only notes that the card needs updating, so it's safe and quick to do without a background thread. */
    @TransactionalEventListener
    void on(SessionScheduled event) {
        refresher.markDirty(event.sessionId());
    }

    @TransactionalEventListener
    void on(SessionAnswered event) {
        refresher.markDirty(event.sessionId());
    }

    @TransactionalEventListener
    void on(SessionChanged event) {
        refresher.markDirty(event.sessionId());
    }

    @TransactionalEventListener
    void on(SessionCancelled event) {
        refresher.markDirty(event.sessionId());
    }

    @TransactionalEventListener
    @Async("telegramExecutor")
    void tellTheChat(SessionChanged event) {
        if (event.startMoved()) {
            logFailure("a moved session", () -> sessionChat.startMoved(event.sessionId()));
        }
    }

    @TransactionalEventListener
    @Async("telegramExecutor")
    void tellTheChat(SessionCancelled event) {
        logFailure("a cancelled session", () -> sessionChat.cancelled(event.sessionId()));
    }

    /** Runs inside the deleting transaction, while the rows can still be read. */
    @EventListener
    void on(AccountDeleting event) {
        events.publishEvent(new AccountDeleted(store.removalsForDeletedAccount(event.studentId())));
    }

    @TransactionalEventListener
    @Async("telegramExecutor")
    void on(AccountDeleted event) {
        logFailure("a deleted account", () -> chatService.onAccountDeleted(event.removals()));
    }

    private static void logFailure(String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            LOG.warn("Could not update Telegram for {}: {}", what, exception.getMessage());
        }
    }

    /**
     * At most two at a time, because each one also needs a database connection and the shared pool is small. The rest
     * wait in a queue, so a slow Telegram never slows the web request that caused the work. If even the queue is full,
     * the update is dropped and logged rather than failing that request.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    @ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
    static class AsyncConfig {

        static final int QUEUE_CAPACITY = 200;

        @Bean("telegramExecutor")
        ThreadPoolTaskExecutor telegramExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setThreadNamePrefix("telegram-sync-");
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(2);
            executor.setQueueCapacity(QUEUE_CAPACITY);
            executor.setRejectedExecutionHandler((task, pool) -> LOG.warn("Too many Telegram updates are waiting, so one was dropped."));
            return executor;
        }
    }
}
