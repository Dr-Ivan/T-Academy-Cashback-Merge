package academy.cashbackmerge.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.RewardTier;
import academy.cashbackmerge.config.SpawnRule;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.GameResult;
import academy.cashbackmerge.strategy.PlayerStrategy;
import academy.cashbackmerge.support.CampaignFixtures;
import academy.cashbackmerge.support.ScriptedRandom;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Автоматическое прохождение одной партии")
class SingleGameRunnerTest {

    @ParameterizedTest(name = "Лимит {0}, максимальная плитка {1}%, score {2}")
    @CsvSource({"1, 1, 0", "2, 2, 2"})
    @DisplayName("Партия выполняет ровно разрешённое число валидных ходов")
    void respectsMovesLimit(int movesLimit, int expectedCashback, long expectedScore) {
        var runner = new SingleGameRunner(smallConfig(movesLimit, 1));
        var result = runner.run(PlayerStrategy.GREEDY_SCORE, 0, new ScriptedRandom(-1));
        assertThat(result).isEqualTo(new GameResult(Cashback.of(expectedCashback), BigDecimal.ZERO, expectedScore));
    }

    @ParameterizedTest(name = "Стратегия {0}")
    @EnumSource(PlayerStrategy.class)
    @DisplayName("Тупик завершает партию до лимита и награда берётся из переданного конфига")
    void stopsAtBlockedBoardAndUsesConfiguredReward(PlayerStrategy strategy) {
        var runner = new SingleGameRunner(smallConfig(100, 32));
        var result = runner.run(strategy, 42, new Random(7));
        assertThat(result).isEqualTo(new GameResult(Cashback.of(32), new BigDecimal("12.50"), 0));
    }

    @ParameterizedTest(name = "Стратегия {0}")
    @EnumSource(PlayerStrategy.class)
    @DisplayName("После получения 32% партия продолжается и выполняет следующие слияния")
    void keepsPlayingAfterReachingMaxTile(PlayerStrategy strategy) {
        var runner = new SingleGameRunner(smallConfig(100, 16));

        var result = runner.run(strategy, 42, new Random(7));

        assertThat(result.maxCashback()).isEqualTo(Cashback.of(32));
        assertThat(result.reward()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(result.score()).isGreaterThan(32);
    }

    @ParameterizedTest(name = "Seed стратегии {0}, score {1}")
    @CsvSource({"7, 2", "42, 4"})
    @DisplayName("Random использует отдельный переданный RNG выбора ходов")
    void usesSuppliedStrategyRandom(long strategySeed, long expectedScore) {
        var runner = new SingleGameRunner(smallConfig(3, 1));
        var result = runner.run(PlayerStrategy.RANDOM, 0, new Random(strategySeed));
        assertThat(result).isEqualTo(new GameResult(Cashback.of(2), BigDecimal.ZERO, expectedScore));
    }

    @ParameterizedTest(name = "Стратегия {0}")
    @EnumSource(PlayerStrategy.class)
    @DisplayName("Повторный запуск с теми же seed воспроизводим и независим от других партий")
    void repeatsIndependentlyOfOtherRuns(PlayerStrategy strategy) {
        var runner = new SingleGameRunner(CampaignFixtures.validConfig());

        var first = runner.run(strategy, -42, new Random(Long.MAX_VALUE));
        runner.run(strategy, 0, new Random(7));
        var repeated = runner.run(strategy, -42, new Random(Long.MAX_VALUE));

        assertThat(repeated).isEqualTo(first);
    }

    private static CampaignConfig smallConfig(int movesLimit, int spawnedCashback) {
        return new CampaignConfig(
                2,
                movesLimit,
                List.of(new SpawnRule(Cashback.of(spawnedCashback), BigDecimal.ONE)),
                List.of(new RewardTier(Cashback.of(32), new BigDecimal("12.50"))),
                Cashback.of(32),
                new BigDecimal("0.20"));
    }
}
