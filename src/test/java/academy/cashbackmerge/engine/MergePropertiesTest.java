package academy.cashbackmerge.engine;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.model.Position;
import academy.cashbackmerge.support.CampaignFixtures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Свойства слияния и перемещения поля")
class MergePropertiesTest {

    private static final List<Integer> VALUES = List.of(0, 1, 2, 4, 8, 16, 32);
    private static final int LINE_SIZE = 4;
    private static final int BOARD_SAMPLES = 64;
    private static final long SEED = 42;

    private final LineMerger merger = new LineMerger();
    private final BoardMover mover = new BoardMover();

    @Test
    @DisplayName("Все 2401 линии длиной четыре соблюдают инварианты слияния и правила score")
    void verifiesPropertiesForEveryFourCellLine() {
        int combinations = 1;
        for (int index = 0; index < LINE_SIZE; index++) {
            combinations *= VALUES.size();
        }

        for (int code = 0; code < combinations; code++) {
            var line = new ArrayList<Optional<Cashback>>(LINE_SIZE);
            int digits = code;

            for (int index = 0; index < LINE_SIZE; index++) {
                line.add(cell(VALUES.get(digits % VALUES.size())));
                digits /= VALUES.size();
            }

            assertLineProperties(line);
        }
    }

    @ParameterizedTest(name = "Размер поля {0} x {0}")
    @ValueSource(ints = {2, 4, 5})
    @DisplayName("Сгенерированные поля сохраняют свойства линий и симметрию четырёх направлений")
    void verifiesPropertiesForGeneratedBoards(int size) {
        var random = new Random(SEED + size);
        var repeatedRandom = new Random(SEED + size);

        for (int sample = 0; sample < BOARD_SAMPLES; sample++) {
            var source = randomBoard(size, random);
            var repeatedSource = randomBoard(size, repeatedRandom);
            var originalTiles = Map.copyOf(source.tiles());
            String context = "Размер " + size + ", пример " + sample;

            assertThat(repeatedSource).as(context + ": повтор генерации").isEqualTo(source);
            for (var move : Move.values()) {
                assertThat(mover.move(repeatedSource, move))
                        .as(context + ": повтор " + move)
                        .isEqualTo(mover.move(source, move));
            }

            for (int row = 0; row < size; row++) {
                assertLineProperties(readRow(source, row));
            }

            var left = mover.move(source, Move.LEFT);
            var right = mover.move(mirrorHorizontally(source), Move.RIGHT);
            var up = mover.move(transpose(source), Move.UP);
            var down = mover.move(transpose(mirrorHorizontally(source)), Move.DOWN);

            for (var result : List.of(left, right, up, down)) {
                assertThat(sum(result.board())).as(context).isEqualTo(sum(source));
            }
            for (int row = 0; row < size; row++) {
                assertSingleMergePerTile(readRow(source, row), readRow(left.board(), row));
            }
            assertThat(left.board().tiles().size())
                    .as(context)
                    .isBetween((source.tiles().size() + 1) / 2, source.tiles().size());
            assertThat(left.scoreDelta()).as(context).isEqualTo(potential(left.board()) - potential(source));
            assertThat(left.changed()).as(context).isEqualTo(!left.board().equals(source));
            assertThat(source.tiles()).as(context).isEqualTo(originalTiles);

            assertTransformedResult(left, right, mirrorHorizontally(left.board()), context + ": RIGHT");
            assertTransformedResult(left, up, transpose(left.board()), context + ": UP");
            assertTransformedResult(left, down, transpose(mirrorHorizontally(left.board())), context + ": DOWN");
        }
    }

    @ParameterizedTest(name = "Seed {0}")
    @ValueSource(longs = {0, 42, -42, Long.MAX_VALUE})
    @DisplayName("Одинаковые seed и ходы воспроизводят начальное поле, перемещения и появление плиток")
    void sameSeedReproducesTileSequence(long seed) {
        var config = CampaignFixtures.validConfig();
        var initializer = new GameInitializer(config);
        var engine = new GameEngine(config);

        var firstRandom = new Random(seed);
        var secondRandom = new Random(seed);
        var first = initializer.initialize(firstRandom);
        var second = initializer.initialize(secondRandom);
        var moves = List.of(Move.UP, Move.LEFT, Move.DOWN, Move.RIGHT);

        assertThat(second).as("Начальное состояние, seed %s", seed).isEqualTo(first);

        for (int step = 0; step < BOARD_SAMPLES; step++) {
            var move = moves.get(step % moves.size());
            var firstMove = engine.applyMove(first, move, firstRandom);
            var secondMove = engine.applyMove(second, move, secondRandom);

            assertThat(secondMove)
                    .as("Результат хода, seed %s, шаг %s, направление %s", seed, step, move)
                    .isEqualTo(firstMove);

            first = firstMove.state();
            second = secondMove.state();

            assertThat(second)
                    .as("Состояние после хода, seed %s, шаг %s", seed, step)
                    .isEqualTo(first);

            if (firstMove.status().isFinished()) {
                break;
            }
        }
    }

    private void assertLineProperties(List<Optional<Cashback>> line) {
        var original = List.copyOf(line);

        var result = merger.merge(line);

        int beforeCount = tileCount(original);
        int afterCount = tileCount(result.cells());

        assertThat(line).as("Исходная линия %s", original).isEqualTo(original);
        assertThat(result.cells()).as("Длина линии %s", original).hasSize(original.size());
        assertThat(sum(result.cells())).as("Сумма линии %s", original).isEqualTo(sum(original));
        assertThat(afterCount).as("Число плиток в %s", original).isBetween((beforeCount + 1) / 2, beforeCount);
        assertThat(result.cells().stream().takeWhile(Optional::isPresent).count())
                .as("Пустоты в %s", original)
                .isEqualTo(afterCount);
        assertSingleMergePerTile(original, result.cells());
        assertThat(result.scoreDelta())
                .as("Score линии %s", original)
                .isEqualTo(potential(result.cells()) - potential(original));
    }

    private static void assertSingleMergePerTile(List<Optional<Cashback>> original, List<Optional<Cashback>> result) {
        var inputs = original.stream().flatMap(Optional::stream).toList();
        var outputs = result.stream().flatMap(Optional::stream).toList();
        String context = "Происхождение плиток: " + inputs + " -> " + outputs;
        int consumed = 0;

        for (var output : outputs) {
            assertThat(consumed).as(context).isLessThan(inputs.size());
            var first = inputs.get(consumed++);

            if (output.value() != first.value()) {
                assertThat(consumed).as(context).isLessThan(inputs.size());
                var second = inputs.get(consumed++);

                assertThat(second).as(context).isEqualTo(first);
                assertThat(output.value()).as(context).isEqualTo(first.value() + second.value());
            }
        }

        assertThat(consumed).as(context).isEqualTo(inputs.size());
    }

    private static void assertTransformedResult(
            BoardMoveResult original, BoardMoveResult transformed, Board expected, String context) {
        assertThat(transformed.board()).as(context).isEqualTo(expected);
        assertThat(transformed.changed()).as(context).isEqualTo(original.changed());
        assertThat(transformed.scoreDelta()).as(context).isEqualTo(original.scoreDelta());
    }

    private static Optional<Cashback> cell(int value) {
        return value == 0 ? Optional.empty() : Optional.of(Cashback.of(value));
    }

    private static List<Optional<Cashback>> readRow(Board board, int row) {
        var line = new ArrayList<Optional<Cashback>>(board.size());
        for (int column = 0; column < board.size(); column++) {
            line.add(board.tileAt(new Position(row, column)));
        }
        return line;
    }

    private static int tileCount(List<Optional<Cashback>> line) {
        return (int) line.stream().filter(Optional::isPresent).count();
    }

    private static long sum(List<Optional<Cashback>> line) {
        return line.stream()
                .flatMap(Optional::stream)
                .mapToLong(Cashback::value)
                .sum();
    }

    private static long sum(Board board) {
        return board.tiles().values().stream().mapToLong(Cashback::value).sum();
    }

    private static long potential(List<Optional<Cashback>> line) {
        return line.stream()
                .flatMap(Optional::stream)
                .mapToLong(MergePropertiesTest::tilePotential)
                .sum();
    }

    private static long potential(Board board) {
        return board.tiles().values().stream()
                .mapToLong(MergePropertiesTest::tilePotential)
                .sum();
    }

    private static long tilePotential(Cashback tile) {
        return (long) tile.value() * Integer.numberOfTrailingZeros(tile.value());
    }

    private static Board randomBoard(int size, Random random) {
        var tiles = new HashMap<Position, Cashback>();

        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int value = VALUES.get(random.nextInt(VALUES.size()));
                if (value != 0) {
                    tiles.put(new Position(row, column), Cashback.of(value));
                }
            }
        }
        return new Board(size, tiles);
    }

    private static Board mirrorHorizontally(Board board) {
        var tiles = new HashMap<Position, Cashback>();
        board.tiles()
                .forEach((position, tile) ->
                        tiles.put(new Position(position.row(), board.size() - 1 - position.column()), tile));
        return new Board(board.size(), tiles);
    }

    private static Board transpose(Board board) {
        var tiles = new HashMap<Position, Cashback>();
        board.tiles().forEach((position, tile) -> tiles.put(new Position(position.column(), position.row()), tile));
        return new Board(board.size(), tiles);
    }
}
