package is442t1.studybuddy.telegram.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import is442t1.studybuddy.telegram.TelegramProperties;
import is442t1.studybuddy.telegram.api.TelegramApiException;
import is442t1.studybuddy.telegram.api.TelegramClient;
import is442t1.studybuddy.telegram.api.Update;

class TelegramUpdatePollerTest {

    private final TelegramClient client = mock(TelegramClient.class);
    private final TelegramUpdateRouter router = mock(TelegramUpdateRouter.class);
    private final TelegramUpdatePoller poller = new TelegramUpdatePoller(client, router,
            new TelegramProperties(true, "1:abc", "bot", true, Duration.ofSeconds(50), Duration.ofMinutes(10), Duration.ofDays(7), Duration.ofMinutes(60)));

    private static TelegramApiException failure(int code, Integer retryAfter) {
        return new TelegramApiException(code, "failed", retryAfter, null);
    }

    @Test
    void movesPastEveryUpdateItRead() {
        Update first = new Update(10, null);
        Update second = new Update(11, null);
        when(client.getUpdates(eq(10L), any(), anyList())).thenReturn(List.of(first, second));

        assertThat(poller.pollOnce(10)).isEqualTo(12);
        verify(router).route(first);
        verify(router).route(second);
    }

    @Test
    void aFailingUpdateIsSkippedAndTheRestStillRun() {
        Update bad = new Update(10, null);
        Update good = new Update(11, null);
        when(client.getUpdates(anyLong(), any(), anyList())).thenReturn(List.of(bad, good));
        doThrow(new IllegalStateException("boom")).when(router).route(bad);

        assertThat(poller.pollOnce(10)).isEqualTo(12);
        verify(router).route(good);
    }

    @Test
    void keepsTheOffsetWhenNothingArrives() {
        when(client.getUpdates(anyLong(), any(), anyList())).thenReturn(List.of());

        assertThat(poller.pollOnce(10)).isEqualTo(10);
    }

    @Test
    void waitsAsLongAsTelegramAsks() {
        assertThat(TelegramUpdatePoller.delayAfter(failure(429, 17), 1)).isEqualTo(Duration.ofSeconds(17));
    }

    @Test
    void waitsAMinuteWhenAnotherPollerHoldsTheToken() {
        assertThat(TelegramUpdatePoller.delayAfter(failure(409, null), 1)).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void backsOffExponentiallyUpToAMinute() {
        TelegramApiException failure = failure(TelegramApiException.NO_REPLY, null);
        assertThat(TelegramUpdatePoller.delayAfter(failure, 1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(TelegramUpdatePoller.delayAfter(failure, 2)).isEqualTo(Duration.ofSeconds(2));
        assertThat(TelegramUpdatePoller.delayAfter(failure, 4)).isEqualTo(Duration.ofSeconds(8));
        assertThat(TelegramUpdatePoller.delayAfter(failure, 50)).isEqualTo(Duration.ofSeconds(60));
    }
}
