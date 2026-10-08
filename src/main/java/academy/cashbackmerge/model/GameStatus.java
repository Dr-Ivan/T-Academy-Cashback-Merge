package academy.cashbackmerge.model;

public enum GameStatus {
    IN_PROGRESS("Партия не завершена."),
    MOVES_LIMIT_REACHED("Партия завершена: достигнут лимит ходов."),
    NO_VALID_MOVES("Партия завершена: нет доступных ходов.");

    private final String message;

    GameStatus(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }

    public boolean isFinished() {
        return this != IN_PROGRESS;
    }
}
