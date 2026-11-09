package academy.cashbackmerge.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.RewardTier;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.strategy.PlayerStrategy;
import academy.cashbackmerge.support.CampaignFixtures;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Сводный отчёт симуляции")
class SimulationReportTest {

    private static final CampaignConfig BASE = CampaignFixtures.validConfig();

    @ParameterizedTest(name = "Максимальных выплат {0} из 5, ограничение {1}, статус {2}")
    @CsvSource({"0, 0, OK", "1, 0, FAILED", "5, 1, OK"})
    @DisplayName("Бизнес-ограничение поддерживает нулевую и стопроцентную допустимую долю")
    void handlesAllowedRateBoundaries(int maxRewardPlayers, String allowedRate, SimulationStatus expected) {
        assertThat(report(5, maxRewardPlayers, allowedRate).status()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Округление доли в отчёте не меняет точный бизнес-статус")
    void checksConstraintBeforeRounding() {
        var report = report(3, 1, "0.3333333");
        assertThat(report.actualMaxRewardRate()).isEqualByComparingTo(new BigDecimal("0.333333"));
        assertThat(report.status()).isEqualTo(SimulationStatus.FAILED);
    }

    @Test
    @DisplayName("Сумма score за пределами long сохраняет точное среднее")
    void averagesScoreWithoutLongOverflow() {
        var total = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO);
        var report = new SimulationReport(
                BASE,
                2,
                42,
                PlayerStrategy.RANDOM,
                Map.of(Cashback.of(2), 2),
                Map.of(BigDecimal.ZERO, 2),
                total,
                Long.MAX_VALUE);

        assertThat(report.averageScore()).isEqualByComparingTo(BigDecimal.valueOf(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("Средний score округляется до шести знаков после запятой")
    void roundsAverageScore() {
        var report = new SimulationReport(
                BASE,
                3,
                42,
                PlayerStrategy.RANDOM,
                Map.of(Cashback.of(2), 3),
                Map.of(BigDecimal.ZERO, 3),
                BigInteger.TWO,
                2);

        assertThat(report.averageScore()).isEqualByComparingTo(new BigDecimal("0.666667"));
    }

    @Test
    @DisplayName("Одинаковые денежные суммы объединяются, распределения независимы от источника и неизменяемы")
    void normalizesRewardsAndCopiesDistributions() {
        var cashbacks = new HashMap<>(Map.of(Cashback.of(32), 2, Cashback.of(2), 1));
        var rewards = new HashMap<BigDecimal, Integer>();
        rewards.put(new BigDecimal("2000.0"), 1);
        rewards.put(new BigDecimal("2000.00"), 1);
        rewards.put(BigDecimal.ZERO, 1);
        var report = new SimulationReport(
                BASE, 3, 42, PlayerStrategy.RANDOM, cashbacks, rewards, BigInteger.valueOf(30), 20);

        cashbacks.clear();
        rewards.clear();

        assertThat(report.maxRewardPlayers()).isEqualTo(2);
        assertThat(report.maxCashbackDistribution())
                .containsExactlyInAnyOrderEntriesOf(Map.of(Cashback.of(32), 2, Cashback.of(2), 1));
        assertThat(report.rewardDistribution())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(new BigDecimal("2000").stripTrailingZeros(), 2, BigDecimal.ZERO, 1));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> report.rewardDistribution().clear());
    }

    @Test
    @DisplayName("Максимальная награда кампании учитывает плитки выше последнего порога")
    void countsHighestPayoutForConfiguredThreshold() {
        var config = new CampaignConfig(
                4,
                100,
                BASE.spawnRules(),
                List.of(
                        new RewardTier(Cashback.of(2), BigDecimal.ZERO),
                        new RewardTier(Cashback.of(16), BigDecimal.TEN)),
                Cashback.of(16),
                new BigDecimal("0.20"));
        var report = new SimulationReport(
                config,
                5,
                42,
                PlayerStrategy.RANDOM,
                Map.of(Cashback.of(8), 3, Cashback.of(16), 1, Cashback.of(32), 1),
                Map.of(BigDecimal.ZERO, 3, BigDecimal.TEN, 2),
                BigInteger.ZERO,
                0);

        assertThat(report.maxRewardPlayers()).isEqualTo(2);
        assertThat(report.actualMaxRewardRate()).isEqualByComparingTo(new BigDecimal("0.4"));
        assertThat(report.status()).isEqualTo(SimulationStatus.FAILED);
    }

    @Test
    @DisplayName("Сумма частот распределения должна совпадать с числом игроков")
    void rejectsIncompleteDistribution() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SimulationReport(
                        BASE,
                        2,
                        42,
                        PlayerStrategy.RANDOM,
                        Map.of(Cashback.of(2), 1),
                        Map.of(BigDecimal.ZERO, 2),
                        BigInteger.ZERO,
                        0))
                .withMessageContaining("maxCashbackDistribution");
    }

    @ParameterizedTest(name = "Сумма score {0}, максимальный score {1}, игроков 2")
    @CsvSource({"1, 2", "5, 2"})
    @DisplayName("Сумма score согласована с максимумом и числом игроков")
    void rejectsImpossibleScores(String totalScore, long maxScore) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SimulationReport(
                        BASE,
                        2,
                        42,
                        PlayerStrategy.RANDOM,
                        Map.of(Cashback.of(2), 2),
                        Map.of(BigDecimal.ZERO, 2),
                        new BigInteger(totalScore),
                        maxScore))
                .withMessageContaining("score");
    }

    @Test
    @DisplayName("Пустая симуляция отклоняется до вычисления средних и долей")
    void rejectsEmptySimulation() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> report(0, 0, "0.20"))
                .withMessageContaining("игроков");
    }

    private static SimulationReport report(int players, int maxRewardPlayers, String allowedRate) {
        var config = new CampaignConfig(
                BASE.boardSize(),
                BASE.movesLimit(),
                BASE.spawnRules(),
                BASE.rewardTable(),
                BASE.maxRewardCashback(),
                new BigDecimal(allowedRate));
        return new SimulationReport(
                config,
                players,
                42,
                PlayerStrategy.RANDOM,
                Map.of(Cashback.of(32), maxRewardPlayers, Cashback.of(2), players - maxRewardPlayers),
                Map.of(new BigDecimal("2000"), maxRewardPlayers, BigDecimal.ZERO, players - maxRewardPlayers),
                BigInteger.ZERO,
                0);
    }
}
