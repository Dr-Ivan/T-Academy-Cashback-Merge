package academy.cashbackmerge.simulation;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.strategy.PlayerStrategy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Неизменяемые параметры и сводная статистика серии партий. */
public record SimulationReport(
        CampaignConfig config,
        int players,
        long seed,
        PlayerStrategy strategy,
        Map<Cashback, Integer> maxCashbackDistribution,
        Map<BigDecimal, Integer> rewardDistribution,
        BigInteger totalScore,
        long maxScore) {

    private static final int DECIMAL_SCALE = 6;

    public SimulationReport {
        Objects.requireNonNull(config, "Конфигурация не должна быть null");
        Objects.requireNonNull(strategy, "Стратегия не должна быть null");
        Objects.requireNonNull(totalScore, "Сумма score не должна быть null");

        if (players <= 0) {
            throw new IllegalArgumentException("Число игроков должно быть положительным: " + players);
        }

        maxCashbackDistribution = Map.copyOf(maxCashbackDistribution);
        rewardDistribution = Map.copyOf(rewardDistribution);
        validateDistribution(maxCashbackDistribution, players, "maxCashbackDistribution");
        validateDistribution(rewardDistribution, players, "rewardDistribution");

        var normalizedRewards = new HashMap<BigDecimal, Integer>();
        for (var entry : rewardDistribution.entrySet()) {
            if (entry.getKey().signum() < 0) {
                throw new IllegalArgumentException("Награды в распределении не должны быть отрицательными");
            }
            normalizedRewards.merge(entry.getKey().stripTrailingZeros(), entry.getValue(), Integer::sum);
        }
        rewardDistribution = Map.copyOf(normalizedRewards);

        if (totalScore.signum() < 0 || maxScore < 0) {
            throw new IllegalArgumentException("Значения score не должны быть отрицательными");
        }
        var maximum = BigInteger.valueOf(maxScore);
        if (totalScore.compareTo(maximum) < 0
                || totalScore.compareTo(maximum.multiply(BigInteger.valueOf(players))) > 0) {
            throw new IllegalArgumentException("Сумма score не согласована с максимальным score и числом игроков");
        }
    }

    public BigDecimal averageScore() {
        return new BigDecimal(totalScore).divide(BigDecimal.valueOf(players), DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    public int maxRewardPlayers() {
        var maxReward = config.rewardTable().getLast().reward();
        return rewardDistribution.getOrDefault(maxReward, 0);
    }

    public BigDecimal actualMaxRewardRate() {
        return BigDecimal.valueOf(maxRewardPlayers())
                .divide(BigDecimal.valueOf(players), DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    public SimulationStatus status() {
        var allowedCount = config.maxRewardAllowedRate().multiply(BigDecimal.valueOf(players));
        return BigDecimal.valueOf(maxRewardPlayers()).compareTo(allowedCount) <= 0
                ? SimulationStatus.OK
                : SimulationStatus.FAILED;
    }

    private static void validateDistribution(Map<?, Integer> distribution, int players, String name) {
        long total = 0;
        for (var count : distribution.values()) {
            if (count < 0) {
                throw new IllegalArgumentException(name + ": число игроков не должно быть отрицательным");
            }
            total += count;
        }
        if (total != players) {
            throw new IllegalArgumentException(name + ": сумма частот должна быть равна числу игроков");
        }
    }
}
