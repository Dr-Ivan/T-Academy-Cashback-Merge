package academy.cashbackmerge.model;

import java.util.Objects;

public record MoveResult(GameState state, boolean changed, long scoreDelta, GameStatus status) {

    public MoveResult {
        Objects.requireNonNull(state, "Состояние игры не должно быть null");
        Objects.requireNonNull(status, "Статус партии не должен быть null");

        if (scoreDelta < 0 || scoreDelta > state.score()) {
            throw new IllegalArgumentException(
                    "scoreDelta должен находиться в диапазоне [0, " + state.score() + "]: " + scoreDelta);
        }

        if (!changed && scoreDelta != 0) {
            throw new IllegalArgumentException("Ход без изменения поля не должен увеличивать score");
        }

        if (changed && state.movesUsed() == 0) {
            throw new IllegalArgumentException("Изменивший поле ход должен быть учтён в movesUsed");
        }

        boolean movesLimitReached = state.movesUsed() == state.movesLimit();
        if ((status == GameStatus.MOVES_LIMIT_REACHED) != movesLimitReached) {
            throw new IllegalArgumentException("Статус завершения по лимиту должен соответствовать счётчику ходов");
        }
    }
}
