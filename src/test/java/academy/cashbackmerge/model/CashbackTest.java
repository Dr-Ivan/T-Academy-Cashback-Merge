package academy.cashbackmerge.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest(name = "Плитки {0}% объединяются в {1}%")
    @CsvSource({"1, 2", "2, 4", "4, 8", "8, 16", "16, 32"})
    @DisplayName("Одинаковые плитки ниже 32% объединяются в следующий уровень")
    void mergesEqualTilesToNextLevel(int value, int expected) {
        var first = Cashback.of(value);
        var second = Cashback.of(value);

        assertThat(first.canMergeWith(second)).isTrue();
        assertThat(first.mergeWith(second)).isEqualTo(Cashback.of(expected));
        assertThat(first.value()).isEqualTo(value);
        assertThat(second.value()).isEqualTo(value);
    }

    @ParameterizedTest(name = "Плитки {0}% и {1}% не объединяются")
    @CsvSource({"1, 2", "16, 32", "32, 32"})
    @DisplayName("Разные уровни и две плитки 32% не объединяются")
    void rejectsForbiddenMerges(int firstValue, int secondValue) {
        var first = Cashback.of(firstValue);
        var second = Cashback.of(secondValue);

        assertThat(first.canMergeWith(second)).isFalse();
        assertThat(second.canMergeWith(first)).isFalse();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> first.mergeWith(second))
                .withMessageContaining("нельзя объединить");
    }
}
