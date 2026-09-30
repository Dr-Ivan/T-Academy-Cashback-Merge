package academy.cashbackmerge.model;

import java.util.Objects;

/** Неизменяемое состояние партии. */
public record GameState(Board board, int movesLimit, int movesUsed, long score) {

    public GameState {
        Objects.requireNonNull(board, "Поле не должно быть null");

        if (movesLimit <= 0) {
            throw new IllegalArgumentException("Лимит ходов должен быть положительным: " + movesLimit);
        }

        if (movesUsed < 0 || movesUsed > movesLimit) {
            throw new IllegalArgumentException(
                    "Количество использованных ходов должно быть от 0 до " + movesLimit + ": " + movesUsed);
        }

        if (score < 0) {
            throw new IllegalArgumentException("Количество очков не должно быть отрицательным: " + score);
        }
    }
}
