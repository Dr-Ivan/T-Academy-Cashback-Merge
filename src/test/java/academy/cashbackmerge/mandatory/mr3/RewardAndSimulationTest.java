package academy.cashbackmerge.mandatory.mr3;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.SpawnRule;
import academy.cashbackmerge.engine.TileSpawner;
import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.GameState;
import academy.cashbackmerge.model.Position;
import academy.cashbackmerge.reward.RewardCalculator;
import academy.cashbackmerge.simulation.SimulationReport;
import academy.cashbackmerge.simulation.SimulationRunner;
import academy.cashbackmerge.simulation.SimulationStatus;
import academy.cashbackmerge.strategy.PlayerStrategy;
import academy.cashbackmerge.support.CampaignFixtures;
import academy.cashbackmerge.support.ScriptedRandom;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: награды, симуляция и отчёт. */
@DisplayName("MR3. Награды и симуляция")
class RewardAndSimulationTest {

    @Test
    @DisplayName("Максимальная плитка 16% даёт награду соответствующего tier")
    void rewardMatchesHighestTile() {
        var board = Board.empty(4)
                .withTile(new Position(0, 0), Cashback.of(4))
                .withTile(new Position(2, 1), Cashback.of(16))
                .withTile(new Position(3, 3), Cashback.of(8));
        var state = new GameState(board, 100, 20, 120);

        var result = new RewardCalculator(CampaignFixtures.validConfig()).calculate(state);

        assertThat(result.maxCashback()).isEqualTo(Cashback.of(16));
        assertThat(result.reward()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(result.score()).isEqualTo(120);
    }

    @Test
    @DisplayName("Weighted random учитывает вероятности из конфига")
    void spawnRespectsConfiguredProbabilities() {
        var base = CampaignFixtures.validConfig();
        var config = new CampaignConfig(
                base.boardSize(),
                base.movesLimit(),
                List.of(
                        new SpawnRule(Cashback.of(4), new BigDecimal("0.25")),
                        new SpawnRule(Cashback.of(1), new BigDecimal("0.50")),
                        new SpawnRule(Cashback.of(2), new BigDecimal("0.25"))),
                base.rewardTable(),
                base.maxRewardCashback(),
                base.maxRewardAllowedRate());
        var spawner = new TileSpawner(config);
        var empty = Board.empty(config.boardSize());
        var counts = new HashMap<Cashback, Integer>();

        for (int ticket = 0; ticket < 100; ticket++) {
            var random = new ScriptedRandom(0, BigInteger.valueOf(ticket));
            var board = spawner.spawn(empty, random);
            var cashback = board.tileAt(new Position(0, 0)).orElseThrow();

            counts.merge(cashback, 1, Integer::sum);
        }

        assertThat(counts)
                .containsExactlyInAnyOrderEntriesOf(Map.of(Cashback.of(4), 25, Cashback.of(1), 50, Cashback.of(2), 25));
    }

    @Test
    @DisplayName("Отчёт содержит распределение максимальных плиток и наград")
    void reportContainsDistributions() {
        var runner = new SimulationRunner(CampaignFixtures.simulationConfig());
        var report = runner.simulate(8, PlayerStrategy.RANDOM, 42);

        assertThat(report.maxCashbackDistribution())
                .containsExactlyInAnyOrderEntriesOf(Map.of(Cashback.of(4), 6, Cashback.of(8), 2));
        assertThat(report.rewardDistribution())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(new BigDecimal("2.5"), 6, BigDecimal.TEN.stripTrailingZeros(), 2));
    }

    @Test
    @DisplayName("Отчёт содержит долю игроков с максимальной наградой")
    void reportContainsMaxRewardRate() {
        var runner = new SimulationRunner(CampaignFixtures.simulationConfig());
        var report = runner.simulate(8, PlayerStrategy.GREEDY_SCORE, 42);

        assertThat(report.players()).isEqualTo(8);
        assertThat(report.maxRewardPlayers()).isEqualTo(3);
        assertThat(report.actualMaxRewardRate()).isEqualByComparingTo(new BigDecimal("0.375"));
    }

    @Test
    @DisplayName("Статус OK, если actualMaxRewardRate <= maxRewardAllowedRate, иначе FAILED")
    void reportStatusReflectsBusinessConstraint() {
        var config = CampaignFixtures.validConfig();
        var allowed = new SimulationReport(
                config,
                5,
                42,
                PlayerStrategy.RANDOM,
                Map.of(Cashback.of(32), 1, Cashback.of(2), 4),
                Map.of(new BigDecimal("2000"), 1, BigDecimal.ZERO, 4),
                BigInteger.ZERO,
                0);
        var exceeded = new SimulationReport(
                config,
                5,
                42,
                PlayerStrategy.RANDOM,
                Map.of(Cashback.of(32), 2, Cashback.of(2), 3),
                Map.of(new BigDecimal("2000"), 2, BigDecimal.ZERO, 3),
                BigInteger.ZERO,
                0);

        assertThat(allowed.status()).isEqualTo(SimulationStatus.OK);
        assertThat(exceeded.status()).isEqualTo(SimulationStatus.FAILED);
    }
}
