package is442t1.studybuddy.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class TelegramPropertiesTest {

    private static TelegramProperties enabled(String token, String username) {
        return new TelegramProperties(true, token, username, true, Duration.ofSeconds(50), Duration.ofMinutes(10), Duration.ofDays(7), Duration.ofMinutes(60));
    }

    @Test
    void offNeedsNoToken() {
        assertThat(new TelegramProperties(false, "", "", true, Duration.ofSeconds(50), Duration.ofMinutes(10), Duration.ofDays(7), Duration.ofMinutes(60)).enabled()).isFalse();
    }

    @Test
    void enabledRefusesABlankOrMalformedToken() {
        assertThatThrownBy(() -> enabled("", "bot")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> enabled("not-a-token", "bot")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enabledRefusesABlankUsername() {
        assertThatThrownBy(() -> enabled("123:abc", " ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void botIdComesFromTheToken() {
        assertThat(enabled("123456:abc-DEF_1", "bot").botId()).isEqualTo(123456L);
    }

    @Test
    void neverPrintsTheToken() {
        assertThat(enabled("123456:secret-value", "bot").toString()).doesNotContain("secret-value");
    }

    @Test
    void aBadTokenErrorDoesNotQuoteTheToken() {
        assertThatThrownBy(() -> enabled("secret value", "bot")).message().doesNotContain("secret value");
    }
}
