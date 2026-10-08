package academy.cashbackmerge.mandatory.mr2;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.engine.BoardMover;
import academy.cashbackmerge.engine.LineMerger;
import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.model.Position;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Обязательные тесты: правила слияния плиток. */
@DisplayName("MR2. Слияние плиток")
class MergeRulesTest {

    private final LineMerger merger = new LineMerger();

    @ParameterizedTest
    @CsvSource({
        "'1,1,.,.', '2,.,.,.'",
        "'1,1,1,.', '2,1,.,.'",
        "'1,1,1,1', '2,2,.,.'",
        "'2,2,4,4', '4,8,.,.'",
    })
    @DisplayName("Сдвиг влево: {0} -> {1}")
    void rowShiftsLeftAndMerges(String before, String expected) {
        var result = merger.merge(parseRow(before));

        assertThat(result.cells()).isEqualTo(parseRow(expected));
    }

    @Test
    @DisplayName("Одна плитка не сливается дважды за ход: [1,1,2,.] -> [2,2,.,.]")
    void tileDoesNotMergeTwiceInOneMove() {
        var result = merger.merge(parseRow("1,1,2,."));

        assertThat(result.cells()).isEqualTo(parseRow("2,2,.,."));
        assertThat(result.scoreDelta()).isEqualTo(2);
    }

    @Test
    @DisplayName("Слияние работает во всех четырёх направлениях")
    void mergeWorksInAllDirections() {
        var tile = Cashback.of(1);
        var merged = Cashback.of(2);
        var source = Board.empty(4)
                .withTile(new Position(1, 1), tile)
                .withTile(new Position(1, 2), tile)
                .withTile(new Position(2, 1), tile)
                .withTile(new Position(2, 2), tile);
        var expectedBoards = Map.of(
                Move.UP,
                Board.empty(4).withTile(new Position(0, 1), merged).withTile(new Position(0, 2), merged),
                Move.DOWN,
                Board.empty(4).withTile(new Position(3, 1), merged).withTile(new Position(3, 2), merged),
                Move.LEFT,
                Board.empty(4).withTile(new Position(1, 0), merged).withTile(new Position(2, 0), merged),
                Move.RIGHT,
                Board.empty(4).withTile(new Position(1, 3), merged).withTile(new Position(2, 3), merged));
        var mover = new BoardMover();

        for (var entry : expectedBoards.entrySet()) {
            var result = mover.move(source, entry.getKey());

            assertThat(result.board()).as("Направление %s", entry.getKey()).isEqualTo(entry.getValue());
            assertThat(result.changed()).isTrue();
            assertThat(result.scoreDelta()).isEqualTo(4);
        }
    }

    private static List<Optional<Cashback>> parseRow(String text) {
        return Arrays.stream(text.split(","))
                .map(cell -> ".".equals(cell)
                        ? Optional.<Cashback>empty()
                        : Optional.of(Cashback.of(Integer.parseInt(cell))))
                .toList();
    }
}
