package academy.cashbackmerge.engine;

import academy.cashbackmerge.model.Board;
import java.util.Objects;

/** Результат перемещения поля до появления новой плитки. */
public record BoardMoveResult(Board board, boolean changed, long scoreDelta) {

    public BoardMoveResult {
        Objects.requireNonNull(board, "Поле не должно быть null");
        if (scoreDelta < 0) {
            throw new IllegalArgumentException("Прибавка к счёту не должна быть отрицательной: " + scoreDelta);
        }
        if (!changed && scoreDelta != 0) {
            throw new IllegalArgumentException("Перемещение без изменения поля не должно увеличивать score");
        }
    }
}
