package academy.cashbackmerge.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Модель значения кэшбэка")
class CashbackTest {

    @ParameterizedTest(name = "Значение {0} принимается")
    @ValueSource(ints = {1, 2, 4, 8, 16, 32})
    @DisplayName("Все разрешённые уровни кэшбэка принимаются")
    void acceptsAllowedCashbackValues(int value) {
        var cashback = Cashback.of(value);
        assertThat(cashback.value()).isEqualTo(value);
    }

    @ParameterizedTest(name = "Значение {0} отклоняется")
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 3, 6, 31, 33, 64, Integer.MAX_VALUE})
    @DisplayName("Значения вне разрешённых уровней кэшбэка отклоняются")
    void rejectsUnsupportedCashbackValues(int value) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Cashback.of(value))
                .withMessageContaining("степенью двойки от 1 до 32");
    }
}
