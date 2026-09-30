package academy.cashbackmerge.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Модель состояния партии")
class GameStateTest {

    @ParameterizedTest(name = "Использовано {0} из 10 ходов")
    @ValueSource(ints = {0, 5, 10})
    @DisplayName("Счётчик ходов допускает начало партии и достижение лимита")
    void acceptsMovesWithinLimit(int movesUsed) {
        var state = new GameState(Board.empty(4), 10, movesUsed, 0);

        assertThat(state.movesUsed()).isEqualTo(movesUsed);
        assertThat(state.movesLimit()).isEqualTo(10);
    }

    @ParameterizedTest(name = "Лимит {0} отклоняется")
    @ValueSource(ints = {-1, 0})
    @DisplayName("Неположительный лимит ходов отклоняется")
    void rejectsNonPositiveMovesLimit(int movesLimit) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GameState(Board.empty(4), movesLimit, 0, 0))
                .withMessageContaining("Лимит ходов");
    }

    @ParameterizedTest(name = "Счётчик {0} отклоняется при лимите 10")
    @ValueSource(ints = {-1, 11})
    @DisplayName("Счётчик ходов вне диапазона от нуля до лимита отклоняется")
    void rejectsMovesOutsideLimit(int movesUsed) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GameState(Board.empty(4), 10, movesUsed, 0))
                .withMessageContaining("использованных ходов");
    }

    @ParameterizedTest(name = "Score {0} принимается")
    @ValueSource(longs = {0, 12, 3_000_000_000L})
    @DisplayName("Неотрицательный score поддерживает значения за пределами int")
    void acceptsNonNegativeScore(long score) {
        var state = new GameState(Board.empty(4), 100, 50, score);
        assertThat(state.score()).isEqualTo(score);
    }

    @ParameterizedTest(name = "Score {0} отклоняется")
    @ValueSource(longs = {-1, Long.MIN_VALUE})
    @DisplayName("Отрицательный score отклоняется")
    void rejectsNegativeScore(long score) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GameState(Board.empty(4), 10, 0, score))
                .withMessageContaining("Количество очков");
    }
}
