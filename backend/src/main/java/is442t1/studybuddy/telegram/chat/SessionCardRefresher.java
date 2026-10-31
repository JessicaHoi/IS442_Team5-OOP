package is442t1.studybuddy.telegram.chat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import is442t1.studybuddy.common.AppProperties;
import is442t1.studybuddy.telegram.api.ReplyMarkup;
import is442t1.studybuddy.telegram.api.TelegramApiException;
import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.chat.ChatStore.ChatRef;
import is442t1.studybuddy.telegram.chat.SessionChatStore.PinChange;
import is442t1.studybuddy.telegram.chat.SessionChatStore.PinnedCard;
import is442t1.studybuddy.telegram.chat.SessionChatStore.SessionRef;

/**
 * The one place that makes a session's card in the chat match the session. Anything that changes a session just marks
 * it dirty. Every few seconds one thread renders each dirty session from current data and posts or edits its card.
 * That merges a burst of presses into one edit, stays inside Telegram's limits, and means an older edit can never
 * overwrite a newer one. Dirty marks are only in memory: a restart loses at most a few seconds, and the next change or
 * the sweep puts it right.
 */
@Component
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
public class SessionCardRefresher {

    private static final Logger LOG = LoggerFactory.getLogger(SessionCardRefresher.class);
    private static final Duration PAUSE_AFTER_OUTAGE = Duration.ofSeconds(30);
    private static final int SERVER_ERRORS = 500;

    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final SessionChatStore store;
    private final ChatStore chatStore;
    private final ChatSender sender;
    private final TelegramClient client;
    private final ZoneId zone;
    private volatile Instant pausedUntil = Instant.MIN;

    SessionCardRefresher(SessionChatStore store, ChatStore chatStore, ChatSender sender, TelegramClient client, AppProperties appProperties) {
        this.store = store;
        this.chatStore = chatStore;
        this.sender = sender;
        this.client = client;
        this.zone = appProperties.timeZone();
    }

    void markDirty(UUID sessionId) {
        dirty.add(sessionId);
    }

    @Scheduled(fixedDelayString = "${studybuddy.telegram.card-refresh}", initialDelayString = "${studybuddy.telegram.card-refresh}")
    void tick() {
        if (!Instant.now().isBefore(pausedUntil)) {
            flush();
        }
    }

    /** One failed card doesn't stop the rest, but when Telegram is struggling everything waits and tries again. */
    public void flush() {
        for (UUID sessionId : List.copyOf(dirty)) {
            // Removed first, so a press that arrives during the edit marks it dirty again rather than being lost.
            dirty.remove(sessionId);
            try {
                refresh(sessionId, Instant.now());
            } catch (TelegramApiException exception) {
                LOG.warn("Could not update a session card: {}", exception.getMessage());
                if (isTemporary(exception)) {
                    dirty.add(sessionId);
                    pausedUntil = Instant.now().plus(exception.retryAfterSeconds() != null
                            ? Duration.ofSeconds(exception.retryAfterSeconds()) : PAUSE_AFTER_OUTAGE);
                    return;
                }
            } catch (RuntimeException exception) {
                LOG.warn("Could not update a session card: {}", exception.getClass().getSimpleName());
            }
        }
    }

    private static boolean isTemporary(TelegramApiException exception) {
        return exception.retryAfterSeconds() != null || exception.code() == TelegramApiException.NO_REPLY || exception.code() >= SERVER_ERRORS;
    }

    private void refresh(UUID sessionId, Instant now) {
        store.read(sessionId).ifPresent(session -> chatStore.findByGroup(session.groupId())
                .filter(chat -> chat.status() == GroupChatStatus.ACTIVE || chat.status() == GroupChatStatus.CLOSED)
                .ifPresent(chat -> refreshIn(chat, sessionId, session, now)));
    }

    private void refreshIn(ChatRef chat, UUID sessionId, SessionRef session, Instant now) {
        SessionMessages.Details details = session.details();
        CardPhase phase = CardPhase.of(session.status(), details.startsAt(), details.endsAt(), now);
        String text = SessionMessages.card(details, phase, zone);
        Long messageId = store.messageId(sessionId).orElse(null);
        if (messageId != null) {
            if (!edit(chat, sessionId, messageId, text, phase)) {
                return;
            }
        } else if (phase == CardPhase.OPEN && chat.status() == GroupChatStatus.ACTIVE) {
            long posted = sender.send(chat, text, SessionMessages.rsvpButtons(sessionId));
            store.recordPost(sessionId, chat.id(), posted, now);
            LOG.info("Posted the card for session {}.", sessionId);
        } else {
            return;
        }
        store.recordShown(sessionId, phase);
        syncPins(chat, now);
    }

    /** Returns false if the message has gone. It's forgotten, so a new one gets posted. */
    private boolean edit(ChatRef chat, UUID sessionId, long messageId, String text, CardPhase phase) {
        try {
            sender.runOnChat(chat, chatId -> {
                if (phase == CardPhase.OPEN) {
                    ReplyMarkup buttons = SessionMessages.rsvpButtons(sessionId);
                    client.editMessage(chatId, messageId, text, buttons);
                } else {
                    client.editMessage(chatId, messageId, text);
                }
            });
            return true;
        } catch (TelegramApiException exception) {
            String reason = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
            if (reason.contains("message is not modified")) {
                return true;
            }
            if (reason.contains("message to edit not found") || reason.contains("message can't be edited")) {
                store.forgetMessage(sessionId);
                dirty.add(sessionId);
                return false;
            }
            throw exception;
        }
    }

    /** Keeps the soonest upcoming session pinned. Nice to have, so a failure is only logged and retried on the next change. */
    private void syncPins(ChatRef chat, Instant now) {
        if (chat.lacks("PIN_MESSAGES")) {
            return;
        }
        PinChange change = store.pinChange(chat.id(), now);
        try {
            if (change.pin() != null) {
                PinnedCard card = change.pin();
                sender.runOnChat(chat, chatId -> client.pinMessage(chatId, card.messageId()));
                store.setPinned(card.sessionId(), true);
            }
            for (PinnedCard card : change.unpin()) {
                sender.runOnChat(chat, chatId -> client.unpinMessage(chatId, card.messageId()));
                store.setPinned(card.sessionId(), false);
            }
        } catch (TelegramApiException exception) {
            LOG.warn("Could not change a pinned session: {}", exception.getMessage());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(prefix = "studybuddy.telegram", name = "enabled", havingValue = "true")
    static class SchedulingConfig {
    }
}
