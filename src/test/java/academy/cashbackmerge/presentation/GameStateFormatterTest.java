package academy.cashbackmerge.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.GameState;
import academy.cashbackmerge.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Текстовое представление состояния партии")
class GameStateFormatterTest {

    @Test
    @DisplayName("Плитки сохраняют координаты, столбцы выровнены, счёт и ходы отображаются полностью")
    void formatsTilesAndGameCounters() {
        var board = Board.empty(2)
                .withTile(new Position(0, 0), Cashback.of(1))
                .withTile(new Position(1, 1), Cashback.of(32));
        var state = new GameState(board, 10, 3, 3_000_000_000L);

        var output = new GameStateFormatter().format(state);

        assertThat(output).isEqualTo("""
                        Счёт: 3000000000
                        Ходы: 3 / 10
                        +-----+-----+
                        |  1% |   . |
                        +-----+-----+
                        |   . | 32% |
                        +-----+-----+
                        """);
    }

    @ParameterizedTest(name = "Поле размером {0} x {0}")
    @ValueSource(ints = {2, 4, 5})
    @DisplayName("Пустое поле отображает все строки и клетки заданного размера")
    void formatsConfiguredBoardSize(int size) {
        var state = new GameState(Board.empty(size), 10, 0, 0);

        var output = new GameStateFormatter().format(state);
        var rows = output.lines().filter(line -> line.startsWith("|")).toList();

        assertThat(output).startsWith("Счёт: 0\nХоды: 0 / 10\n");
        assertThat(rows).hasSize(size);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.chars().filter(symbol -> symbol == '.').count()).isEqualTo(size);
            assertThat(row.chars().filter(symbol -> symbol == '|').count()).isEqualTo(size + 1);
        });
    }
}
