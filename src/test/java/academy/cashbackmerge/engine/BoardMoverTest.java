package academy.cashbackmerge.engine;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.model.Position;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Перемещение игрового поля")
class BoardMoverTest {

    private final BoardMover mover = new BoardMover();

    @Test
    @DisplayName("Сдвиг без слияний изменяет поле, сохраняет плитки и не начисляет очки")
    void shiftsWithoutMergingOrSpawning() {
        var source =
                board(new int[] {0, 1, 0, 0}, new int[] {0, 0, 0, 0}, new int[] {0, 0, 0, 2}, new int[] {0, 0, 0, 0});
        var expected =
                board(new int[] {1, 0, 0, 0}, new int[] {0, 0, 0, 0}, new int[] {2, 0, 0, 0}, new int[] {0, 0, 0, 0});

        var result = mover.move(source, Move.LEFT);

        assertThat(result.board()).isEqualTo(expected);
        assertThat(result.changed()).isTrue();
        assertThat(result.scoreDelta()).isZero();
    }

    @Test
    @DisplayName("Очки за независимые слияния разных линий складываются, исходное поле сохраняется")
    void sumsScoreAcrossLinesAndPreservesSource() {
        var source =
                board(new int[] {1, 1, 2, 2}, new int[] {0, 0, 0, 0}, new int[] {8, 8, 16, 16}, new int[] {0, 0, 0, 0});
        var expected =
                board(new int[] {2, 4, 0, 0}, new int[] {0, 0, 0, 0}, new int[] {16, 32, 0, 0}, new int[] {0, 0, 0, 0});

        var result = mover.move(source, Move.LEFT);

        assertThat(result.board()).isEqualTo(expected);
        assertThat(result.changed()).isTrue();
        assertThat(result.scoreDelta()).isEqualTo(54);
        assertThat(source.tiles()).hasSize(8);
        assertThat(source.tileAt(new Position(2, 3))).contains(Cashback.of(16));
    }

    @Test
    @DisplayName("Направление без сдвигов и слияний возвращает исходное поле и нулевую прибавку")
    void returnsOriginalBoardForUnchangedMove() {
        var source =
                board(new int[] {1, 2, 0, 0}, new int[] {4, 8, 0, 0}, new int[] {0, 0, 0, 0}, new int[] {0, 0, 0, 0});

        var result = mover.move(source, Move.LEFT);

        assertThat(result.board()).isSameAs(source);
        assertThat(result.changed()).isFalse();
        assertThat(result.scoreDelta()).isZero();
    }

    @ParameterizedTest(name = "Направление {0}")
    @EnumSource(Move.class)
    @DisplayName("Полное поле из плиток 32% не меняется ни в одном направлении")
    void doesNotMergeMaxTiles(Move move) {
        var source = board(new int[] {32, 32}, new int[] {32, 32});

        var result = mover.move(source, move);

        assertThat(result.board()).isSameAs(source);
        assertThat(result.changed()).isFalse();
        assertThat(result.scoreDelta()).isZero();
    }

    @ParameterizedTest(name = "Размер поля {0} x {0}")
    @ValueSource(ints = {2, 4, 5})
    @DisplayName("Сдвиги вправо и вниз учитывают размер поля из конфигурации")
    void usesConfiguredBoardSize(int size) {
        int last = size - 1;
        var horizontal = Board.empty(size)
                .withTile(new Position(last, 0), Cashback.of(1))
                .withTile(new Position(last, last), Cashback.of(1));
        var vertical = Board.empty(size)
                .withTile(new Position(0, last), Cashback.of(1))
                .withTile(new Position(last, last), Cashback.of(1));
        var expected = Board.empty(size).withTile(new Position(last, last), Cashback.of(2));

        var right = mover.move(horizontal, Move.RIGHT);
        var down = mover.move(vertical, Move.DOWN);

        assertThat(right.board()).isEqualTo(expected);
        assertThat(right.changed()).isTrue();
        assertThat(right.scoreDelta()).isEqualTo(2);
        assertThat(down.board()).isEqualTo(expected);
        assertThat(down.changed()).isTrue();
        assertThat(down.scoreDelta()).isEqualTo(2);
    }

    private static Board board(int[]... rows) {
        var tiles = new HashMap<Position, Cashback>();

        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < rows.length; column++) {
                int value = rows[row][column];
                if (value != 0) {
                    tiles.put(new Position(row, column), Cashback.of(value));
                }
            }
        }
        return new Board(rows.length, tiles);
    }
}
