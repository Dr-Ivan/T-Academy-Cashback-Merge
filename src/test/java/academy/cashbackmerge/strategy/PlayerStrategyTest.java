package academy.cashbackmerge.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import academy.cashbackmerge.engine.BoardMover;
import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.model.Position;
import academy.cashbackmerge.support.ScriptedRandom;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Стратегии автоматического игрока")
class PlayerStrategyTest {

    @ParameterizedTest(name = "Индекс {0} выбирает {1}")
    @CsvSource({"0, DOWN", "1, RIGHT"})
    @DisplayName("Random выбирает направление по индексу среди доступных ходов")
    void randomSelectsOnlyAvailableMoves(int index, Move expected) {
        var board = boardWithTwoDifferentTiles();
        var move = PlayerStrategy.RANDOM.chooseMove(board, new ScriptedRandom(index));
        assertThat(move).isEqualTo(expected);
    }

    @Test
    @DisplayName("Greedy выбирает наибольшую прибавку к score и первое направление при равенстве")
    void greedyChoosesHighestScoreAndKeepsFirstTie() {
        var board = Board.empty(4)
                .withTile(new Position(0, 0), Cashback.of(8))
                .withTile(new Position(0, 1), Cashback.of(8))
                .withTile(new Position(1, 2), Cashback.of(1))
                .withTile(new Position(2, 2), Cashback.of(1));

        var move = PlayerStrategy.GREEDY_SCORE.chooseMove(board, new ScriptedRandom(-1));

        assertThat(move).isEqualTo(Move.LEFT);
        assertThat(new BoardMover().move(board, move).scoreDelta()).isEqualTo(16);
    }

    @Test
    @DisplayName("Greedy учитывает вертикальные слияния и сохраняет приоритет UP при равенстве")
    void greedyChoosesVerticalMerge() {
        var board = Board.empty(2)
                .withTile(new Position(0, 0), Cashback.of(1))
                .withTile(new Position(1, 0), Cashback.of(1));

        var move = PlayerStrategy.GREEDY_SCORE.chooseMove(board, new ScriptedRandom(-1));
        assertThat(move).isEqualTo(Move.UP);
    }

    @Test
    @DisplayName("При нулевых прибавках Greedy выбирает первое доступное направление")
    void greedyChoosesValidMoveWhenEveryScoreIsZero() {
        var move = PlayerStrategy.GREEDY_SCORE.chooseMove(boardWithTwoDifferentTiles(), new ScriptedRandom(-1));
        assertThat(move).isEqualTo(Move.DOWN);
    }

    @ParameterizedTest(name = "Стратегия {0}")
    @EnumSource(PlayerStrategy.class)
    @DisplayName("Заблокированное поле отклоняется до обращения к RNG")
    void rejectsBlockedBoardWithoutConsumingRandomness(PlayerStrategy strategy) {
        var board = Board.empty(2)
                .withTile(new Position(0, 0), Cashback.of(32))
                .withTile(new Position(0, 1), Cashback.of(32))
                .withTile(new Position(1, 0), Cashback.of(32))
                .withTile(new Position(1, 1), Cashback.of(32));

        assertThatIllegalStateException()
                .isThrownBy(() -> strategy.chooseMove(board, new ScriptedRandom(-1)))
                .withMessageContaining("нет доступных ходов");
    }

    @ParameterizedTest(name = "Стратегия {0}")
    @EnumSource(PlayerStrategy.class)
    @DisplayName("Выбор сохраняет исходное поле и возвращает изменяющий его ход")
    void preservesBoardAndReturnsValidMove(PlayerStrategy strategy) {
        var board = boardWithTwoDifferentTiles();
        var originalTiles = Map.copyOf(board.tiles());

        var move = strategy.chooseMove(board, new ScriptedRandom(0));

        assertThat(board.tiles()).isEqualTo(originalTiles);
        assertThat(new BoardMover().move(board, move).changed()).isTrue();
    }

    @ParameterizedTest(name = "Размер поля {0} x {0}")
    @ValueSource(ints = {2, 4, 5})
    @DisplayName("Обе стратегии поддерживают размер поля из конфигурации")
    void supportsConfiguredBoardSize(int size) {
        var board = Board.empty(size).withTile(new Position(0, 0), Cashback.of(1));

        assertThat(PlayerStrategy.RANDOM.chooseMove(board, new ScriptedRandom(1)))
                .isEqualTo(Move.RIGHT);
        assertThat(PlayerStrategy.GREEDY_SCORE.chooseMove(board, new ScriptedRandom(-1)))
                .isEqualTo(Move.DOWN);
    }

    @Test
    @DisplayName("Одинаковый seed воспроизводит последовательность выборов Random")
    void reproducesRandomChoicesWithSameSeed() {
        var board = boardWithTwoDifferentTiles();
        var first = new Random(42);
        var second = new Random(42);

        for (int step = 0; step < 16; step++) {
            assertThat(PlayerStrategy.RANDOM.chooseMove(board, second))
                    .as("Выбор %s", step)
                    .isEqualTo(PlayerStrategy.RANDOM.chooseMove(board, first));
        }
    }

    private static Board boardWithTwoDifferentTiles() {
        return Board.empty(4).withTile(new Position(0, 0), Cashback.of(1)).withTile(new Position(0, 1), Cashback.of(2));
    }
}
