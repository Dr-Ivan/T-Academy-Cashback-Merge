package academy.cashbackmerge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.GameState;
import academy.cashbackmerge.model.GameStatus;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.model.Position;
import academy.cashbackmerge.support.CampaignFixtures;
import academy.cashbackmerge.support.ScriptedRandom;
import java.math.BigInteger;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Граничные случаи движка ходов")
class GameEngineTest {

    private final GameEngine engine = new GameEngine(CampaignFixtures.validConfig());

    @ParameterizedTest(name = "Размер поля {0} x {0}")
    @ValueSource(ints = {2, 4, 5})
    @DisplayName("Полное поле из плиток 32% завершает партию без расходования RNG")
    void finishesWhenMaxTilesBlockEveryDirection(int size) {
        var board = Board.empty(size);
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                board = board.withTile(new Position(row, column), Cashback.of(32));
            }
        }
        var state = new GameState(board, 7, 1, 0);

        var result = engine.applyMove(state, Move.LEFT, new ScriptedRandom(-1));

        assertThat(engine.status(state)).isEqualTo(GameStatus.NO_VALID_MOVES);
        assertThat(result.status()).isEqualTo(GameStatus.NO_VALID_MOVES);
        assertThat(result.state()).isSameAs(state);
        assertThat(result.changed()).isFalse();
        assertThat(result.scoreDelta()).isZero();
    }

    @Test
    @DisplayName("Пустое поле не допускает ходов, несмотря на наличие свободных клеток")
    void detectsEmptyBoardWithoutSpawning() {
        var state = new GameState(Board.empty(4), 7, 0, 0);

        var result = engine.applyMove(state, Move.UP, new ScriptedRandom(-1));

        assertThat(engine.status(state)).isEqualTo(GameStatus.NO_VALID_MOVES);
        assertThat(result.status()).isEqualTo(GameStatus.NO_VALID_MOVES);
        assertThat(result.state()).isSameAs(state);
        assertThat(result.changed()).isFalse();
        assertThat(result.scoreDelta()).isZero();
    }

    @Test
    @DisplayName("Полное поле с доступным слиянием позволяет продолжать игру")
    void doesNotTreatFullBoardAsBlockedWhenMergeIsPossible() {
        var board = Board.empty(2)
                .withTile(new Position(0, 0), Cashback.of(1))
                .withTile(new Position(0, 1), Cashback.of(1))
                .withTile(new Position(1, 0), Cashback.of(2))
                .withTile(new Position(1, 1), Cashback.of(4));
        var state = new GameState(board, 7, 0, 0);

        assertThat(engine.status(state)).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @ParameterizedTest(name = "Лимит ходов {0}")
    @ValueSource(ints = {1, 7})
    @DisplayName("Тупик проверяется после появления плитки, завершение по лимиту имеет приоритет")
    void detectsBlockedBoardAfterSpawnAndPrioritizesMovesLimit(int movesLimit) {
        var board = Board.empty(2)
                .withTile(new Position(0, 0), Cashback.of(1))
                .withTile(new Position(0, 1), Cashback.of(2))
                .withTile(new Position(1, 0), Cashback.of(4));
        var state = new GameState(board, movesLimit, 0, 10);
        var random = new ScriptedRandom(0, BigInteger.valueOf(85));
        var expected = Board.empty(2)
                .withTile(new Position(0, 0), Cashback.of(1))
                .withTile(new Position(0, 1), Cashback.of(2))
                .withTile(new Position(1, 0), Cashback.of(2))
                .withTile(new Position(1, 1), Cashback.of(4));
        var expectedStatus = movesLimit == 1 ? GameStatus.MOVES_LIMIT_REACHED : GameStatus.NO_VALID_MOVES;

        var result = engine.applyMove(state, Move.RIGHT, random);
        var afterFinish = engine.applyMove(result.state(), Move.LEFT, new ScriptedRandom(-1));

        assertThat(result.state().board()).isEqualTo(expected);
        assertThat(result.state().movesUsed()).isEqualTo(1);
        assertThat(result.state().score()).isEqualTo(10);
        assertThat(result.changed()).isTrue();
        assertThat(result.scoreDelta()).isZero();
        assertThat(result.status()).isEqualTo(expectedStatus);
        assertThat(engine.status(result.state())).isEqualTo(expectedStatus);
        assertThat(random.samplesUsed()).isEqualTo(1);
        assertThat(afterFinish.state()).isSameAs(result.state());
        assertThat(afterFinish.changed()).isFalse();
        assertThat(afterFinish.scoreDelta()).isZero();
        assertThat(afterFinish.status()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest(name = "Начальный score {0}")
    @ValueSource(longs = {3_000_000_000L, Long.MAX_VALUE - 2})
    @DisplayName("Накопленный score сохраняет точность long вплоть до максимального значения")
    void preservesLongScoreWithoutTruncation(long initialScore) {
        var board = Board.empty(4)
                .withTile(new Position(0, 0), Cashback.of(1))
                .withTile(new Position(0, 1), Cashback.of(1));
        var state = new GameState(board, 7, 0, initialScore);

        var result = engine.applyMove(state, Move.LEFT, new ScriptedRandom(0, BigInteger.ZERO));

        assertThat(result.scoreDelta()).isEqualTo(2);
        assertThat(result.state().score()).isEqualTo(initialScore + 2);
        assertThat(result.changed()).isTrue();
        assertThat(result.state().movesUsed()).isEqualTo(1);
    }

    @Test
    @DisplayName("Переполнение score обнаруживается до расходования RNG")
    void rejectsScoreOverflowBeforeSpawning() {
        var board = Board.empty(4)
                .withTile(new Position(0, 0), Cashback.of(1))
                .withTile(new Position(0, 1), Cashback.of(1));
        var state = new GameState(board, 7, 0, Long.MAX_VALUE - 1);

        assertThatExceptionOfType(ArithmeticException.class)
                .isThrownBy(() -> engine.applyMove(state, Move.LEFT, new ScriptedRandom(-1)));
        assertThat(state.score()).isEqualTo(Long.MAX_VALUE - 1);
        assertThat(state.movesUsed()).isZero();
        assertThat(state.board()).isSameAs(board);
    }

    @Test
    @DisplayName("Невалидный ход не меняет результат следующего хода при одинаковом seed")
    void invalidMoveDoesNotAdvanceRandomSequence() {
        var board = Board.empty(4)
                .withTile(new Position(0, 0), Cashback.of(1))
                .withTile(new Position(0, 1), Cashback.of(2));
        var state = new GameState(board, 7, 0, 0);
        var firstRandom = new Random(42);
        var secondRandom = new Random(42);

        var invalid = engine.applyMove(state, Move.LEFT, firstRandom);
        var afterInvalid = engine.applyMove(invalid.state(), Move.RIGHT, firstRandom);
        var withoutInvalid = engine.applyMove(state, Move.RIGHT, secondRandom);

        assertThat(invalid.state()).isSameAs(state);
        assertThat(invalid.changed()).isFalse();
        assertThat(afterInvalid).isEqualTo(withoutInvalid);
    }
}
