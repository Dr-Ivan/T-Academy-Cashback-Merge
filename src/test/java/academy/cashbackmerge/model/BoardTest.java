package academy.cashbackmerge.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Модель игрового поля")
class BoardTest {

    @ParameterizedTest(name = "Поле размером {0} x {0}")
    @ValueSource(ints = {2, 4, 5})
    @DisplayName("Размер поля задаётся при создании")
    void createsEmptyBoardOfRequestedSize(int size) {
        var board = Board.empty(size);

        assertThat(board.size()).isEqualTo(size);
        assertThat(board.tiles()).isEmpty();
        assertThat(board.tileAt(new Position(size - 1, size - 1))).isEmpty();
    }

    @ParameterizedTest(name = "Размер {0} отклоняется")
    @ValueSource(ints = {-1, 0, 1})
    @DisplayName("Поле меньше двух клеток по стороне отклоняется")
    void rejectsTooSmallBoard(int size) {
        assertThatIllegalArgumentException().isThrownBy(() -> Board.empty(size));
    }

    @ParameterizedTest(name = "Координаты ({0}, {1}) отклоняются")
    @CsvSource({"-1, 0", "0, -1"})
    @DisplayName("Отрицательные координаты отклоняются")
    void rejectsNegativeCoordinates(int row, int column) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Position(row, column));
    }

    @ParameterizedTest(name = "Клетка ({0}, {1}) вне поля")
    @CsvSource({"4, 0", "0, 4", "4, 4"})
    @DisplayName("Клетки за границами поля отклоняются при создании, чтении и добавлении")
    void rejectsPositionsOutsideBoard(int row, int column) {
        var board = Board.empty(4);
        var position = new Position(row, column);
        var cashback = Cashback.of(1);

        assertThatIllegalArgumentException().isThrownBy(() -> new Board(4, Map.of(position, cashback)));
        assertThatIllegalArgumentException().isThrownBy(() -> board.tileAt(position));
        assertThatIllegalArgumentException().isThrownBy(() -> board.withTile(position, cashback));
    }

    @Test
    @DisplayName("Добавление плитки создаёт новое поле и сохраняет исходное")
    void addingTilePreservesOriginalBoard() {
        var original = Board.empty(4);
        var position = new Position(3, 3);
        var cashback = Cashback.of(2);

        var updated = original.withTile(position, cashback);

        assertThat(original.tiles()).isEmpty();
        assertThat(updated.tiles()).hasSize(1);
        assertThat(updated.tileAt(position)).contains(cashback);
    }

    @Test
    @DisplayName("Добавление плитки в занятую клетку отклоняется")
    void rejectsOccupiedPosition() {
        var position = new Position(0, 0);
        var board = Board.empty(4).withTile(position, Cashback.of(1));

        assertThatIllegalArgumentException().isThrownBy(() -> board.withTile(position, Cashback.of(2)));

        assertThat(board.tileAt(position)).contains(Cashback.of(1));
    }

    @Test
    @DisplayName("Изменение исходной карты не меняет поле")
    void protectsBoardFromChangesToSourceMap() {
        var position = new Position(0, 0);
        var cashback = Cashback.of(1);
        var source = new HashMap<Position, Cashback>();
        source.put(position, cashback);
        var board = new Board(4, source);

        source.clear();

        assertThat(board.tiles()).containsExactlyEntriesOf(Map.of(position, cashback));
    }

    @Test
    @DisplayName("Карту плиток нельзя изменить через поле")
    void exposesUnmodifiableTiles() {
        var board = Board.empty(4);

        assertThatThrownBy(() -> board.tiles().put(new Position(0, 0), Cashback.of(1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
