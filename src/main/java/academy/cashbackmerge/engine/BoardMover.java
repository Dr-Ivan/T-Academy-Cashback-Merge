package academy.cashbackmerge.engine;

import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.model.Position;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Перемещение всего поля с единым алгоритмом слияния для четырёх направлений. */
public final class BoardMover {

    private final LineMerger lineMerger = new LineMerger();

    public BoardMoveResult move(Board board, Move move) {
        Objects.requireNonNull(board, "Поле не должно быть null");
        Objects.requireNonNull(move, "Направление не должно быть null");

        var tiles = new HashMap<Position, Cashback>();
        long scoreDelta = 0;

        for (int lineIndex = 0; lineIndex < board.size(); lineIndex++) {
            var lineResult = lineMerger.merge(readLine(board, move, lineIndex));
            scoreDelta += lineResult.scoreDelta();

            for (int offset = 0; offset < board.size(); offset++) {
                var position = move.position(lineIndex, offset, board.size());
                lineResult.cells().get(offset).ifPresent(tile -> tiles.put(position, tile));
            }
        }

        boolean changed = !tiles.equals(board.tiles());
        var resultBoard = changed ? new Board(board.size(), tiles) : board;
        return new BoardMoveResult(resultBoard, changed, scoreDelta);
    }

    private static List<Optional<Cashback>> readLine(Board board, Move move, int lineIndex) {
        var line = new ArrayList<Optional<Cashback>>(board.size());

        for (int offset = 0; offset < board.size(); offset++) {
            line.add(board.tileAt(move.position(lineIndex, offset, board.size())));
        }
        return line;
    }
}
