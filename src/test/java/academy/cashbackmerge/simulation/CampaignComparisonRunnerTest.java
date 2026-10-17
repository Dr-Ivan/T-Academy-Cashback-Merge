package academy.cashbackmerge.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.strategy.PlayerStrategy;
import academy.cashbackmerge.support.CampaignFixtures;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Сравнение нескольких кампаний")
class CampaignComparisonRunnerTest {

    private static final CampaignConfig BASE = CampaignFixtures.simulationConfig();

    @ParameterizedTest(name = "Стратегия {0}")
    @EnumSource(PlayerStrategy.class)
    @DisplayName("Каждая кампания получает общие параметры, результаты упорядочены по имени")
    void comparesWithSharedInputs(PlayerStrategy strategy) {
        var other = otherConfig();

        var comparison = new CampaignComparisonRunner().compare(Map.of("B", other, "A", BASE), 8, strategy, -42);

        assertThat(comparison.players()).isEqualTo(8);
        assertThat(comparison.seed()).isEqualTo(-42);
        assertThat(comparison.strategy()).isEqualTo(strategy);
        assertThat(comparison.campaigns()).extracting(CampaignResult::name).containsExactly("A", "B");
        assertThat(comparison.campaigns().getFirst().report())
                .isEqualTo(new SimulationRunner(BASE).simulate(8, strategy, -42));
        assertThat(comparison.campaigns().getLast().report())
                .isEqualTo(new SimulationRunner(other).simulate(8, strategy, -42));
    }

    @ParameterizedTest(name = "Стратегия {0}")
    @EnumSource(PlayerStrategy.class)
    @DisplayName("Одинаковые конфигурации под разными именами получают одинаковые результаты")
    void usesSameSeedsForEveryCampaign(PlayerStrategy strategy) {
        var comparison =
                new CampaignComparisonRunner().compare(Map.of("A", BASE, "B", BASE), 8, strategy, Long.MAX_VALUE);

        assertThat(comparison.campaigns().getFirst().report())
                .isEqualTo(comparison.campaigns().getLast().report());
    }

    @Test
    @DisplayName("Порядок входной Map, другие запуски и дополнительные кампании не меняют результаты")
    void repeatsIndependentlyOfOtherCampaignsAndRuns() {
        var runner = new CampaignComparisonRunner();
        var other = otherConfig();
        var first = runner.compare(Map.of("C", other, "A", BASE), 8, PlayerStrategy.RANDOM, 7);

        runner.compare(Map.of("X", other, "Y", BASE), 2, PlayerStrategy.GREEDY_SCORE, 0);
        var repeated = runner.compare(Map.of("A", BASE, "C", other), 8, PlayerStrategy.RANDOM, 7);
        var extended = runner.compare(Map.of("B", BASE, "C", other, "A", BASE), 8, PlayerStrategy.RANDOM, 7);

        assertThat(repeated).isEqualTo(first);
        assertThat(extended.campaigns().stream()
                        .filter(campaign -> !"B".equals(campaign.name()))
                        .toList())
                .isEqualTo(first.campaigns());
    }

    @ParameterizedTest(name = "Число кампаний {0}")
    @ValueSource(ints = {0, 1})
    @DisplayName("Для сравнения необходимы хотя бы две кампании")
    void rejectsTooFewCampaigns(int count) {
        Map<String, CampaignConfig> configs = count == 0 ? Map.of() : Map.of("A", BASE);
        List<CampaignResult> results = count == 0
                ? List.of()
                : List.of(new CampaignResult("A", new SimulationRunner(BASE).simulate(1, PlayerStrategy.RANDOM, 42)));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CampaignComparisonRunner().compare(configs, 1, PlayerStrategy.RANDOM, 42))
                .withMessageContaining("две");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CampaignComparisonReport(results))
                .withMessageContaining("две");
    }

    @ParameterizedTest(name = "Число игроков {0}")
    @ValueSource(ints = {0, -1})
    @DisplayName("Число игроков в сравнении должно быть положительным")
    void rejectsNonPositivePlayers(int players) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CampaignComparisonRunner()
                        .compare(Map.of("A", BASE, "B", BASE), players, PlayerStrategy.RANDOM, 42))
                .withMessageContaining("игроков");
    }

    @Test
    @DisplayName("Пустые имена и отсутствующие конфигурации отклоняются")
    void rejectsInvalidCampaigns() {
        var runner = new CampaignComparisonRunner();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> runner.compare(Map.of(" ", BASE, "B", BASE), 1, PlayerStrategy.RANDOM, 42))
                .withMessageContaining("Имя");
        var configs = new HashMap<String, CampaignConfig>();
        configs.put("A", BASE);
        configs.put("B", null);
        assertThatNullPointerException().isThrownBy(() -> runner.compare(configs, 1, PlayerStrategy.RANDOM, 42));
    }

    @Test
    @DisplayName("Сравнение не зависит от исходных коллекций и содержит неизменяемый список")
    void copiesInputCollections() {
        var configs = new HashMap<>(Map.of("A", BASE, "B", otherConfig()));
        var original = new CampaignComparisonRunner().compare(configs, 3, PlayerStrategy.RANDOM, 42);
        var campaigns = new ArrayList<>(original.campaigns());
        var copy = new CampaignComparisonReport(campaigns);

        configs.clear();
        campaigns.clear();

        assertThat(copy).isEqualTo(original);
        assertThat(copy.campaigns()).hasSize(2);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> copy.campaigns().clear());
    }

    @ParameterizedTest(name = "Игроков {0}, seed {1}, стратегия {2}")
    @CsvSource({"2, 42, RANDOM", "1, 43, RANDOM", "1, 42, GREEDY_SCORE"})
    @DisplayName("Отчёт отклоняет разные число игроков, seed или стратегию")
    void rejectsInconsistentParameters(int players, long seed, PlayerStrategy strategy) {
        var first = new CampaignResult("A", new SimulationRunner(BASE).simulate(1, PlayerStrategy.RANDOM, 42));
        var second = new CampaignResult("B", new SimulationRunner(BASE).simulate(players, strategy, seed));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CampaignComparisonReport(List.of(first, second)))
                .withMessageContaining("одинаковые");
    }

    @Test
    @DisplayName("Имена кампаний в готовом сравнении не должны повторяться")
    void rejectsDuplicateNames() {
        var result = new CampaignResult("A", new SimulationRunner(BASE).simulate(1, PlayerStrategy.RANDOM, 42));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CampaignComparisonReport(List.of(result, result)))
                .withMessageContaining("повторяется");
    }

    private static CampaignConfig otherConfig() {
        return new CampaignConfig(
                3, 5, BASE.spawnRules(), BASE.rewardTable(), BASE.maxRewardCashback(), new BigDecimal("0.50"));
    }
}
