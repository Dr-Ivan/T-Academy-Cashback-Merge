package academy.cashbackmerge.simulation;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.engine.GameSession;
import academy.cashbackmerge.model.GameResult;
import academy.cashbackmerge.reward.RewardCalculator;
import academy.cashbackmerge.strategy.PlayerStrategy;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Полное прохождение одной партии выбранной стратегией. */
public final class SingleGameRunner {

    private final CampaignConfig config;
    private final RewardCalculator rewardCalculator;

    public SingleGameRunner(CampaignConfig config) {
        this.config = Objects.requireNonNull(config, "Конфигурация не должна быть null");
        this.rewardCalculator = new RewardCalculator(config);
    }

    /** Seed управляет плитками, переданный RNG используется только для выбора ходов. */
    public GameResult run(PlayerStrategy strategy, long gameSeed, RandomGenerator strategyRandom) {
        Objects.requireNonNull(strategy, "Стратегия не должна быть null");
        Objects.requireNonNull(strategyRandom, "Генератор стратегии не должен быть null");

        var session = new GameSession(config, gameSeed);
        var status = session.status();

        while (!status.isFinished()) {
            var move = strategy.chooseMove(session.state().board(), strategyRandom);
            var result = session.applyMove(move);
            if (!result.changed()) {
                throw new IllegalStateException("Стратегия выбрала ход без изменения поля");
            }
            status = result.status();
        }

        return rewardCalculator.calculate(session.state());
    }
}
