package academy.cashbackmerge.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Результат хода и статус партии")
class MoveResultTest {

    @ParameterizedTest(name = "Прибавка к score: {0}")
    @ValueSource(longs = {0, 12, 3_000_000_000L})
    @DisplayName("Валидный ход допускает нулевую прибавку и значения score за пределами int")
    void acceptsScoreDeltaForChangedBoard(long scoreDelta) {
        var state = activeState(1, scoreDelta);

        var result = new MoveResult(state, true, scoreDelta, GameStatus.IN_PROGRESS);

        assertThat(result.changed()).isTrue();
        assertThat(result.scoreDelta()).isEqualTo(scoreDelta);
        assertThat(result.status().isFinished()).isFalse();
    }

    @Test
    @DisplayName("Последний валидный ход одновременно изменяет поле и завершает партию")
    void changedMoveCanFinishGame() {
        var state = activeState(7, 12);

        var result = new MoveResult(state, true, 12, GameStatus.MOVES_LIMIT_REACHED);

        assertThat(result.changed()).isTrue();
        assertThat(result.status().isFinished()).isTrue();
    }

    @ParameterizedTest(name = "Статус {0}, партия завершена: {1}")
    @CsvSource({"IN_PROGRESS, false", "MOVES_LIMIT_REACHED, true", "NO_VALID_MOVES, true"})
    @DisplayName("Попытка без изменения поля допустима в продолжающейся и завершённой партии")
    void acceptsUnchangedResult(GameStatus status, boolean finished) {
        var state = status == GameStatus.NO_VALID_MOVES
                ? blockedState()
                : activeState(status == GameStatus.MOVES_LIMIT_REACHED ? 7 : 1, 12);

        var result = new MoveResult(state, false, 0, status);

        assertThat(result.state()).isSameAs(state);
        assertThat(result.changed()).isFalse();
        assertThat(result.scoreDelta()).isZero();
        assertThat(result.status().isFinished()).isEqualTo(finished);
    }

    @ParameterizedTest(name = "Прибавка {0} отклоняется при общем score 12")
    @ValueSource(longs = {-1, 13})
    @DisplayName("Прибавка к score не может быть отрицательной или превышать итоговый счёт")
    void rejectsScoreDeltaOutsideRange(long scoreDelta) {
        var state = activeState(1, 12);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MoveResult(state, true, scoreDelta, GameStatus.IN_PROGRESS))
                .withMessageContaining("scoreDelta");
    }

    @Test
    @DisplayName("Попытка без изменения поля не может начислять очки")
    void rejectsScoreDeltaForUnchangedBoard() {
        var state = activeState(1, 12);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MoveResult(state, false, 2, GameStatus.IN_PROGRESS))
                .withMessageContaining("без изменения поля");
    }

    @Test
    @DisplayName("Изменивший поле ход должен быть отражён в счётчике ходов")
    void rejectsChangedBoardWithoutUsedMove() {
        var state = activeState(0, 0);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MoveResult(state, true, 0, GameStatus.IN_PROGRESS))
                .withMessageContaining("movesUsed");
    }

    @ParameterizedTest(name = "Использовано {0} из 7 ходов, статус {1}")
    @CsvSource({"6, MOVES_LIMIT_REACHED", "7, IN_PROGRESS", "7, NO_VALID_MOVES"})
    @DisplayName("Статус завершения по лимиту согласован со счётчиком ходов")
    void rejectsStatusInconsistentWithMovesLimit(int movesUsed, GameStatus status) {
        var state = activeState(movesUsed, 12);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MoveResult(state, false, 0, status))
                .withMessageContaining("лимиту");
    }

    private static GameState activeState(int movesUsed, long score) {
        var board = Board.empty(2).withTile(new Position(0, 0), Cashback.of(1));
        return new GameState(board, 7, movesUsed, score);
    }

    private static GameState blockedState() {
        var board = Board.empty(2);

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                board = board.withTile(new Position(row, column), Cashback.of(32));
            }
        }
        return new GameState(board, 7, 1, 12);
    }
}
