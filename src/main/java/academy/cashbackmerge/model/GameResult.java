package academy.cashbackmerge.model;

import java.math.BigDecimal;
import java.util.Objects;

public record GameResult(Cashback maxCashback, BigDecimal reward, long score) {

    public GameResult {
        Objects.requireNonNull(maxCashback, "Максимальная плитка не должна быть null");
        Objects.requireNonNull(reward, "Награда не должна быть null");

        if (reward.signum() < 0) {
            throw new IllegalArgumentException("Награда не должна быть отрицательной: " + reward);
        }
        if (score < 0) {
            throw new IllegalArgumentException("Score не должен быть отрицательным: " + score);
        }

        reward = reward.stripTrailingZeros();
    }
}
