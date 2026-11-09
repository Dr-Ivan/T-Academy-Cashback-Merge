package academy.cashbackmerge.reward;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.RewardTier;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.GameResult;
import academy.cashbackmerge.model.GameState;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Расчет награды по максимальной плитке и порогам кампании. */
public final class RewardCalculator {

    private final List<RewardTier> rewardTable;

    public RewardCalculator(CampaignConfig config) {
        this.rewardTable = Objects.requireNonNull(config, "Конфигурация не должна быть null")
                .rewardTable();
    }

    public GameResult calculate(GameState state) {
        Objects.requireNonNull(state, "Состояние игры не должно быть null");

        var maxCashback = state.board().tiles().values().stream()
                .max(Comparator.comparingInt(Cashback::value))
                .orElseThrow(() -> new IllegalArgumentException("Нельзя рассчитать награду для пустого поля"));

        var reward = BigDecimal.ZERO;
        for (var tier : rewardTable) {
            if (tier.cashback().value() > maxCashback.value()) {
                break;
            }
            reward = tier.reward();
        }

        return new GameResult(maxCashback, reward, state.score());
    }
}
