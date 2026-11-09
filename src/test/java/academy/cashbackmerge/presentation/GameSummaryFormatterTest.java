package academy.cashbackmerge.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.GameResult;
import academy.cashbackmerge.model.GameState;
import academy.cashbackmerge.model.GameStatus;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.model.Position;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Итог партии с максимальной плиткой и наградой")
class GameSummaryFormatterTest {

    private final GameSummaryFormatter formatter = new GameSummaryFormatter();

    @Test
    @DisplayName("Итог сохраняет поле, score, данные replay и точную денежную награду")
    void formatsCompleteSummary() {
        var board = Board.empty(2)
                .withTile(new Position(0, 0), Cashback.of(16))
                .withTile(new Position(1, 1), Cashback.of(16));
        var state = new GameState(board, 5, 5, 3_000_000_000L);
        var result = new GameResult(Cashback.of(16), new BigDecimal("12345678901234567890.123456789"), state.score());

        var output = formatter.format(-42, state, GameStatus.MOVES_LIMIT_REACHED, List.of(Move.LEFT, Move.UP), result);

        assertThat(output).isEqualTo("""
                Seed: -42
                Ходы для replay: LEFT,UP
                Счёт: 3000000000
                Ходы: 5 / 5
                +-----+-----+
                | 16% |   . |
                +-----+-----+
                |   . | 16% |
                +-----+-----+
                Максимальная плитка: 16%
                Итоговая награда: 12345678901234567890.123456789
                Партия завершена: достигнут лимит ходов.
                """);
    }

    @ParameterizedTest(name = "Завершение: {0}")
    @EnumSource(
            value = GameStatus.class,
            names = {"MOVES_LIMIT_REACHED", "NO_VALID_MOVES"})
    @DisplayName("Обе причины завершения показывают итоговую награду без научной записи")
    void labelsFinishedReward(GameStatus status) {
        var board = Board.empty(2)
                .withTile(new Position(0, 0), Cashback.of(32))
                .withTile(new Position(0, 1), Cashback.of(32))
                .withTile(new Position(1, 0), Cashback.of(32))
                .withTile(new Position(1, 1), Cashback.of(32));
        int used = status == GameStatus.MOVES_LIMIT_REACHED ? 5 : 2;
        var state = new GameState(board, 5, used, 0);
        var result = new GameResult(Cashback.of(32), new BigDecimal("2500.00"), 0);

        assertThat(formatter.format(0, state, status, List.of(), result))
                .contains(
                        "Seed: 0\n",
                        "Ходы для replay: (пустой список)\n",
                        "Максимальная плитка: 32%\n",
                        "Итоговая награда: 2500\n",
                        status.message())
                .doesNotContain("Награда по текущему полю", "E+");
    }

    @ParameterizedTest(name = "Награда {0} отображается как {1}")
    @CsvSource({"0.0000, 0", "2500.00, 2500", "0.10000000000000000001, 0.10000000000000000001"})
    @DisplayName("Незавершённая партия показывает награду по текущему полю, включая нулевую")
    void labelsCurrentReward(String amount, String expected) {
        var board = Board.empty(2).withTile(new Position(0, 0), Cashback.of(32));
        var state = new GameState(board, 5, 2, 0);
        var result = new GameResult(Cashback.of(32), new BigDecimal(amount), 0);

        assertThat(formatter.format(42, state, GameStatus.IN_PROGRESS, List.of(), result))
                .contains(
                        "Максимальная плитка: 32%\n",
                        "Награда по текущему полю: " + expected + "\n", "Партия не завершена.")
                .doesNotContain("Итоговая награда", "E+");
    }
}
