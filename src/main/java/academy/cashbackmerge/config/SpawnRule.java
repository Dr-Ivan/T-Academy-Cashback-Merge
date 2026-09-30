package academy.cashbackmerge.config;

import academy.cashbackmerge.model.Cashback;
import java.math.BigDecimal;
import java.util.Objects;

public record SpawnRule(Cashback cashback, BigDecimal probability) {

    public SpawnRule {
        Objects.requireNonNull(cashback, "spawnRules.cashback не должен быть null");
        Objects.requireNonNull(probability, "spawnRules.probability не должна быть null");

        if (probability.signum() < 0 || probability.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "spawnRules.probability должна находиться в диапазоне [0, 1]: " + probability);
        }

        probability = probability.stripTrailingZeros();
    }
}
