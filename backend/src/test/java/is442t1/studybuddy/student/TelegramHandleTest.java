package is442t1.studybuddy.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import is442t1.studybuddy.common.exception.ValidationException;

class TelegramHandleTest {

    @ParameterizedTest
    @ValueSource(strings = {"aisha_r", "@aisha_r", "  @aisha_r  ", "t.me/aisha_r", "https://t.me/aisha_r", "HTTP://www.T.me/aisha_r"})
    void acceptsAUsernameInTheFormsStudentsPaste(String typed) {
        assertThat(TelegramHandle.normalize(typed)).isEqualTo("aisha_r");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "1abcde", "_abcde", "abcde_", "has space", "a-b-c-d-e", "@@abcde", "https://example.com/abcde"})
    void rejectsWhatTelegramDoesNotAllow(String typed) {
        assertThatThrownBy(() -> TelegramHandle.normalize(typed)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsAUsernameLongerThan32Characters() {
        assertThat(TelegramHandle.normalize("a".repeat(32))).hasSize(32);
        assertThatThrownBy(() -> TelegramHandle.normalize("a".repeat(33))).isInstanceOf(ValidationException.class);
    }

    @Test
    void treatsAnEmptyFieldAsNoHandle() {
        assertThat(TelegramHandle.normalize(null)).isNull();
        assertThat(TelegramHandle.normalize("   ")).isNull();
    }
}
