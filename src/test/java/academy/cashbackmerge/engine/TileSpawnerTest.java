package academy.cashbackmerge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.SpawnRule;
import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.Position;
import academy.cashbackmerge.support.CampaignFixtures;
import academy.cashbackmerge.support.ScriptedRandom;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Появление плиток")
class TileSpawnerTest {

    private static final CampaignConfig BASE = CampaignFixtures.validConfig();

    @ParameterizedTest(name = "Выборка {0} даёт плитку {1}%")
    @CsvSource({"0, 1", "84, 1", "85, 2", "99, 2"})
    @DisplayName("Границы интервалов соответствуют вероятностям из конфигурации")
    void selectsCashbackByConfiguredIntervals(int sample, int expected) {
        var spawner = new TileSpawner(BASE);
        var random = new ScriptedRandom(0, BigInteger.valueOf(sample));

        var result = spawner.spawn(Board.empty(2), random);

        assertThat(result.tileAt(new Position(0, 0))).contains(Cashback.of(expected));
    }

    @ParameterizedTest(name = "Индекс {0} выбирает клетку ({1}, {2})")
    @CsvSource({"0, 0, 1", "1, 1, 0", "2, 1, 1"})
    @DisplayName("Свободные клетки перечисляются по строкам, занятые плитки сохраняются")
    void selectsFreePositionInStableOrder(int index, int row, int column) {
        var original = Board.empty(2).withTile(new Position(0, 0), Cashback.of(4));
        var random = new ScriptedRandom(index, BigInteger.ZERO);

        var result = new TileSpawner(BASE).spawn(original, random);

        assertThat(result.tiles()).hasSize(2).containsAllEntriesOf(original.tiles());
        assertThat(result.tileAt(new Position(row, column))).contains(Cashback.of(1));
        assertThat(original.tiles()).hasSize(1);
    }

    @ParameterizedTest(name = "Вероятности {0}/{1} дают плитку {2}%")
    @CsvSource({"0, 1, 2", "1, 0, 1"})
    @DisplayName("Плитка с нулевой вероятностью не выбирается")
    void skipsZeroProbability(String firstProbability, String secondProbability, int expected) {
        var config = withRules(List.of(
                new SpawnRule(Cashback.of(1), new BigDecimal(firstProbability)),
                new SpawnRule(Cashback.of(2), new BigDecimal(secondProbability))));
        var random = new ScriptedRandom(0);

        var result = new TileSpawner(config).spawn(Board.empty(2), random);

        assertThat(result.tileAt(new Position(0, 0))).contains(Cashback.of(expected));
    }

    @ParameterizedTest(name = "Выборка {0} даёт плитку {1}%")
    @CsvSource({"99999999999999999998, 1", "99999999999999999999, 2"})
    @DisplayName("Вероятности высокой точности сохраняют интервалы за пределами long")
    void supportsWeightsBeyondLongRange(String sample, int expected) {
        var config = withRules(List.of(
                new SpawnRule(Cashback.of(1), new BigDecimal("0.99999999999999999999")),
                new SpawnRule(Cashback.of(2), new BigDecimal("0.00000000000000000001"))));
        var random = new ScriptedRandom(0, new BigInteger(sample));
        var result = new TileSpawner(config).spawn(Board.empty(2), random);
        assertThat(result.tileAt(new Position(0, 0))).contains(Cashback.of(expected));
    }

    @Test
    @DisplayName("Выборка вне диапазона отбрасывается и выбор повторяется")
    void rejectsSampleOutsideWeightRange() {
        var random = new ScriptedRandom(0, BigInteger.valueOf(100), BigInteger.valueOf(85));

        var result = new TileSpawner(BASE).spawn(Board.empty(2), random);

        assertThat(result.tileAt(new Position(0, 0))).contains(Cashback.of(2));
        assertThat(random.samplesUsed()).isEqualTo(2);
    }

    @Test
    @DisplayName("Заполненное поле отклоняется до случайного выбора")
    void rejectsFullBoardWithoutConsumingRandomness() {
        var board = new Board(
                2,
                Map.of(
                        new Position(0, 0), Cashback.of(1),
                        new Position(0, 1), Cashback.of(2),
                        new Position(1, 0), Cashback.of(4),
                        new Position(1, 1), Cashback.of(8)));
        var random = new ScriptedRandom(0);

        assertThatIllegalStateException()
                .isThrownBy(() -> new TileSpawner(BASE).spawn(board, random))
                .withMessageContaining("нет свободных клеток");

        assertThat(random.samplesUsed()).isZero();
    }

    private static CampaignConfig withRules(List<SpawnRule> rules) {
        return new CampaignConfig(
                BASE.boardSize(),
                BASE.movesLimit(),
                rules,
                BASE.rewardTable(),
                BASE.maxRewardCashback(),
                BASE.maxRewardAllowedRate());
    }
}
