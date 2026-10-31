package is442t1.studybuddy.telegram.bot;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import is442t1.studybuddy.telegram.TelegramProperties;
import is442t1.studybuddy.telegram.api.TelegramApiException;
import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.api.Update;

/**
 * Reads updates from Telegram on one background thread for as long as the application runs. An update that
 * fails is logged and skipped, so one bad update can't block the ones behind it.
 */
@Component
@ConditionalOnProperty(prefix = "studybuddy.telegram", name = {"enabled", "polling"}, havingValue = "true")
class TelegramUpdatePoller implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramUpdatePoller.class);
    private static final int CONFLICT = 409;
    private static final List<String> ALLOWED_UPDATES = List.of("message", "my_chat_member", "chat_member", "chat_join_request", "callback_query");
    private static final Duration CONFLICT_WAIT = Duration.ofSeconds(60);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(60);
    private static final Duration STOP_WAIT = Duration.ofSeconds(5);

    private final TelegramClient client;
    private final TelegramUpdateRouter router;
    private final TelegramProperties properties;

    private volatile boolean running;
    private volatile Thread thread;

    TelegramUpdatePoller(TelegramClient client, TelegramUpdateRouter router, TelegramProperties properties) {
        this.client = client;
        this.router = router;
        this.properties = properties;
    }

    @Override
    public void start() {
        running = true;
        thread = Thread.ofVirtual().name("telegram-poller").start(this::run);
    }

    @Override
    public void stop() {
        running = false;
        Thread current = thread;
        if (current == null) {
            return;
        }
        current.interrupt();
        try {
            current.join(STOP_WAIT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void run() {
        boolean prepared = false;
        long offset = 0;
        int failures = 0;
        while (running) {
            try {
                if (!prepared) {
                    prepare();
                    prepared = true;
                }
                offset = pollOnce(offset);
                failures = 0;
            } catch (TelegramApiException exception) {
                if (!running) {
                    return;
                }
                failures++;
                if (exception.code() == CONFLICT) {
                    LOG.warn("Another poller or webhook uses this bot token. Only one instance may poll per token.");
                } else {
                    LOG.warn("Telegram polling failed: {}", exception.getMessage());
                }
                if (!sleep(delayAfter(exception, failures))) {
                    return;
                }
            }
        }
    }

    private void prepare() {
        client.getMe();
        client.deleteWebhook();
        LOG.info("Telegram bot @{} is polling for updates.", properties.botUsername());
    }

    /** Handles one batch of updates and returns the next offset. */
    long pollOnce(long offset) {
        long next = offset;
        for (Update update : client.getUpdates(offset, properties.pollTimeout(), ALLOWED_UPDATES)) {
            next = update.updateId() + 1;
            try {
                router.route(update);
            } catch (TelegramApiException exception) {
                // Safe to log: these messages hold Telegram's error text, and never message text or the token.
                LOG.error("Could not handle Telegram update {}: {}", update.updateId(), exception.getMessage());
            } catch (RuntimeException exception) {
                LOG.error("Could not handle Telegram update {}: {}", update.updateId(), exception.getClass().getSimpleName());
            }
        }
        return next;
    }

    /** Telegram's own wait if it gave one; otherwise doubles from one second, up to a minute. */
    static Duration delayAfter(TelegramApiException exception, int failures) {
        if (exception.retryAfterSeconds() != null) {
            return Duration.ofSeconds(exception.retryAfterSeconds());
        }
        if (exception.code() == CONFLICT) {
            return CONFLICT_WAIT;
        }
        Duration backoff = Duration.ofSeconds(1L << Math.min(failures - 1, 6));
        return backoff.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : backoff;
    }

    private static boolean sleep(Duration duration) {
        try {
            Thread.sleep(duration);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
