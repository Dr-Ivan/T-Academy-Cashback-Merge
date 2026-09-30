package academy.cashbackmerge.config;

import academy.cashbackmerge.model.Cashback;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Проверенная неизменяемая конфигурация кампании. */
public record CampaignConfig(
        int boardSize,
        int movesLimit,
        List<SpawnRule> spawnRules,
        List<RewardTier> rewardTable,
        Cashback maxRewardCashback,
        BigDecimal maxRewardAllowedRate) {

    private static final int MIN_BOARD_SIZE = 2;

    public CampaignConfig {
        if (boardSize < MIN_BOARD_SIZE) {
            throw new IllegalArgumentException("boardSize должен быть не меньше " + MIN_BOARD_SIZE + ": " + boardSize);
        }

        if (movesLimit <= 0) {
            throw new IllegalArgumentException("movesLimit должен быть положительным: " + movesLimit);
        }

        Objects.requireNonNull(maxRewardCashback, "maxRewardCashback не должен быть null");
        Objects.requireNonNull(maxRewardAllowedRate, "maxRewardAllowedRate не должна быть null");

        spawnRules = List.copyOf(spawnRules);
        validateSpawnRules(spawnRules);

        rewardTable = List.copyOf(rewardTable).stream()
                .sorted(Comparator.comparingInt(tier -> tier.cashback().value()))
                .toList();
        validateRewardTable(rewardTable, maxRewardCashback);

        if (maxRewardAllowedRate.signum() < 0 || maxRewardAllowedRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "maxRewardAllowedRate должна находиться в диапазоне [0, 1]: " + maxRewardAllowedRate);
        }

        maxRewardAllowedRate = maxRewardAllowedRate.stripTrailingZeros();
    }

    private static void validateSpawnRules(List<SpawnRule> rules) {
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("spawnRules не должны быть пустыми");
        }

        var cashbacks = new HashSet<Cashback>();
        var totalProbability = BigDecimal.ZERO;

        for (var rule : rules) {
            if (!cashbacks.add(rule.cashback())) {
                throw new IllegalArgumentException("spawnRules содержат повторяющийся уровень кэшбэка: "
                        + rule.cashback().value());
            }

            totalProbability = totalProbability.add(rule.probability());
        }

        if (totalProbability.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException(
                    "Сумма вероятностей spawnRules должна быть равна 1: " + totalProbability.toPlainString());
        }
    }

    private static void validateRewardTable(List<RewardTier> tiers, Cashback maxRewardCashback) {
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("rewardTable не должна быть пустой");
        }

        int previousCashback = 0;
        var previousReward = BigDecimal.ZERO;

        for (var tier : tiers) {
            if (tier.cashback().value() == previousCashback) {
                throw new IllegalArgumentException(
                        "rewardTable содержит повторяющийся уровень кэшбэка: " + previousCashback);
            }

            if (tier.reward().compareTo(previousReward) < 0) {
                throw new IllegalArgumentException("Награды rewardTable не должны уменьшаться с ростом кэшбэка");
            }

            previousCashback = tier.cashback().value();
            previousReward = tier.reward();
        }

        var highestTier = tiers.getLast();

        if (!highestTier.cashback().equals(maxRewardCashback)) {
            throw new IllegalArgumentException("maxRewardCashback должен совпадать с последним порогом rewardTable");
        }

        if (tiers.size() > 1
                && highestTier.reward().compareTo(tiers.get(tiers.size() - 2).reward()) == 0) {
            throw new IllegalArgumentException("Максимальная награда rewardTable должна быть строго выше предыдущей");
        }
    }
}
