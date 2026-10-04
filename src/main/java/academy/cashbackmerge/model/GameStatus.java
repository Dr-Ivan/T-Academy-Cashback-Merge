package academy.cashbackmerge.model;

public enum GameStatus {
    IN_PROGRESS,
    MOVES_LIMIT_REACHED,
    NO_VALID_MOVES;

    public boolean isFinished() {
        return this != IN_PROGRESS;
    }
}
