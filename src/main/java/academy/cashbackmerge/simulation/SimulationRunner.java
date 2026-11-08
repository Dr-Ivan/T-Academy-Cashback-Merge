package academy.cashbackmerge.simulation;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.strategy.PlayerStrategy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

/** Воспроизводимый запуск серии партий с накоплением итоговой статистики. */
public final class SimulationRunner {

    private final CampaignConfig config;
    private final SingleGameRunner gameRunner;

    public SimulationRunner(CampaignConfig config) {
        this.config = Objects.requireNonNull(config, "Конфигурация не должна быть null");
        this.gameRunner = new SingleGameRunner(config);
    }

    public SimulationReport simulate(int players, PlayerStrategy strategy, long seed) {
        if (players <= 0) {
            throw new IllegalArgumentException("Число игроков должно быть положительным: " + players);
        }
        Objects.requireNonNull(strategy, "Стратегия не должна быть null");

        var seedRandom = new Random(seed);
        var cashbacks = new HashMap<Cashback, Integer>();
        var rewards = new HashMap<BigDecimal, Integer>();
        var totalScore = BigInteger.ZERO;
        long maxScore = 0;

        for (int player = 0; player < players; player++) {
            var gameSeed = seedRandom.nextLong();
            var strategySeed = seedRandom.nextLong();
            var result = gameRunner.run(strategy, gameSeed, new Random(strategySeed));

            cashbacks.merge(result.maxCashback(), 1, Integer::sum);
            rewards.merge(result.reward(), 1, Integer::sum);
            totalScore = totalScore.add(BigInteger.valueOf(result.score()));
            maxScore = Math.max(maxScore, result.score());
        }

        return new SimulationReport(config, players, seed, strategy, cashbacks, rewards, totalScore, maxScore);
    }
}
