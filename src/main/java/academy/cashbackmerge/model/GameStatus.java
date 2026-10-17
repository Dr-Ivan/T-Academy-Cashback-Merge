package academy.cashbackmerge.model;

public enum GameStatus {
    IN_PROGRESS("Партия не завершена.", "Награда по текущему полю"),
    MOVES_LIMIT_REACHED("Партия завершена: достигнут лимит ходов.", "Итоговая награда"),
    NO_VALID_MOVES("Партия завершена: нет доступных ходов.", "Итоговая награда");

    private final String message;
    private final String rewardLabel;

    GameStatus(String message, String rewardLabel) {
        this.message = message;
        this.rewardLabel = rewardLabel;
    }

    public String message() {
        return message;
    }

    public boolean isFinished() {
        return this != IN_PROGRESS;
    }

    public String rewardLabel() {
        return rewardLabel;
    }
}
