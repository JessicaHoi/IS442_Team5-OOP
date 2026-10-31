package is442t1.studybuddy.telegram.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class TelegramExecutorTest {

    @Test
    void aSaturatedExecutorNeverBlocksOrFailsTheCaller() throws Exception {
        ThreadPoolTaskExecutor executor = new ChatSyncListener.AsyncConfig().telegramExecutor();
        executor.initialize();
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger finished = new AtomicInteger();
        int running = 2;
        int dropped = 5;

        long started = System.nanoTime();
        for (int task = 0; task < running + ChatSyncListener.AsyncConfig.QUEUE_CAPACITY + dropped; task++) {
            executor.execute(() -> {
                try {
                    release.await();
                    finished.incrementAndGet();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)).isLessThan(5);

        release.countDown();
        executor.setAwaitTerminationSeconds(10);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.shutdown();
        assertThat(finished.get()).isEqualTo(running + ChatSyncListener.AsyncConfig.QUEUE_CAPACITY);
    }
}
