package academy.cashbackmerge.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import academy.cashbackmerge.strategy.PlayerStrategy;
import academy.cashbackmerge.support.CampaignFixtures;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Серия автоматических партий")
class SimulationRunnerTest {

    @ParameterizedTest(name = "{0}: сумма score {1}, максимум {2}, среднее {3}")
    @CsvSource({"RANDOM, 90, 20, 11.25", "GREEDY_SCORE, 112, 24, 14"})
    @DisplayName("Серия сохраняет параметры запуска и правильно накапливает score")
    void aggregatesScoresAndPreservesParameters(
            PlayerStrategy strategy, long expectedTotal, long expectedMax, String expectedAverage) {
        var config = CampaignFixtures.simulationConfig();

        var report = new SimulationRunner(config).simulate(8, strategy, 42);

        assertThat(report.config()).isEqualTo(config);
        assertThat(report.players()).isEqualTo(8);
        assertThat(report.seed()).isEqualTo(42);
        assertThat(report.strategy()).isEqualTo(strategy);
        assertThat(report.totalScore()).isEqualTo(BigInteger.valueOf(expectedTotal));
        assertThat(report.maxScore()).isEqualTo(expectedMax);
        assertThat(report.averageScore()).isEqualByComparingTo(new BigDecimal(expectedAverage));
    }

    @ParameterizedTest(name = "Стратегия {0}")
    @EnumSource(PlayerStrategy.class)
    @DisplayName("Игрок получает отдельные seed партии и стратегии из общего seed")
    void usesSeparateGameAndStrategySeeds(PlayerStrategy strategy) {
        var config = CampaignFixtures.simulationConfig();
        var expected =
                new SingleGameRunner(config).run(strategy, -5025562857975149833L, new Random(-5843495416241995736L));
        var report = new SimulationRunner(config).simulate(1, strategy, 42);

        assertThat(report)
                .isEqualTo(new SimulationReport(
                        config,
                        1,
                        42,
                        strategy,
                        Map.of(expected.maxCashback(), 1),
                        Map.of(expected.reward(), 1),
                        BigInteger.valueOf(expected.score()),
                        expected.score()));
    }

    @ParameterizedTest(name = "Число игроков {0}")
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    @DisplayName("Число игроков должно быть положительным")
    void rejectsNonPositivePlayers(int players) {
        var runner = new SimulationRunner(CampaignFixtures.simulationConfig());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> runner.simulate(players, PlayerStrategy.RANDOM, 42))
                .withMessageContaining("игроков");
    }

    @Test
    @DisplayName("Конфигурация и стратегия обязательны")
    void rejectsMissingArguments() {
        assertThatNullPointerException().isThrownBy(() -> new SimulationRunner(null));
        var runner = new SimulationRunner(CampaignFixtures.simulationConfig());
        assertThatNullPointerException().isThrownBy(() -> runner.simulate(1, null, 42));
    }
}
