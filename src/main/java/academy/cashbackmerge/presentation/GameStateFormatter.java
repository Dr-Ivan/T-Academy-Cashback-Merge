package academy.cashbackmerge.presentation;

import academy.cashbackmerge.model.GameState;
import academy.cashbackmerge.model.Position;
import java.util.Objects;

/** Текстовое представление поля, счёта и использованных ходов. */
public final class GameStateFormatter {

    private static final int CELL_WIDTH = 3;

    public String format(GameState state) {
        Objects.requireNonNull(state, "Состояние игры не должно быть null");

        var board = state.board();
        var output = new StringBuilder()
                .append("Счёт: ")
                .append(state.score())
                .append("\nХоды: ")
                .append(state.movesUsed())
                .append(" / ")
                .append(state.movesLimit())
                .append('\n');

        var border = ("+" + "-".repeat(CELL_WIDTH + 2)).repeat(board.size()) + "+\n";
        output.append(border);

        for (int row = 0; row < board.size(); row++) {
            output.append('|');
            for (int column = 0; column < board.size(); column++) {
                var cell = board.tileAt(new Position(row, column))
                        .map(tile -> tile.value() + "%")
                        .orElse(".");
                output.append(' ')
                        .repeat(" ", CELL_WIDTH - cell.length())
                        .append(cell)
                        .append(" |");
            }
            output.append('\n').append(border);
        }

        return output.toString();
    }
}
