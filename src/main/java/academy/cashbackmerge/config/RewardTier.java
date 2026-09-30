package academy.cashbackmerge.config;

import academy.cashbackmerge.model.Cashback;
import java.math.BigDecimal;
import java.util.Objects;

public record RewardTier(Cashback cashback, BigDecimal reward) {

    public RewardTier {
        Objects.requireNonNull(cashback, "rewardTable.cashback не должен быть null");
        Objects.requireNonNull(reward, "rewardTable.reward не должна быть null");

        if (reward.signum() < 0) {
            throw new IllegalArgumentException("rewardTable.reward не должна быть отрицательной: " + reward);
        }

        reward = reward.stripTrailingZeros();
    }
}
