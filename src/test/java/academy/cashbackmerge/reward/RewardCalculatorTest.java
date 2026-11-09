package academy.cashbackmerge.reward;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.RewardTier;
import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.GameResult;
import academy.cashbackmerge.model.GameState;
import academy.cashbackmerge.model.Position;
import academy.cashbackmerge.support.CampaignFixtures;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Расчёт денежной награды")
class RewardCalculatorTest {

    private static final CampaignConfig BASE = CampaignFixtures.validConfig();
    private final RewardCalculator calculator = new RewardCalculator(BASE);

    @ParameterizedTest(name = "Плитка {0}% даёт награду {1}")
    @CsvSource({"1, 0", "2, 0", "4, 50", "8, 150", "16, 500", "32, 2000"})
    @DisplayName("Награда соответствует максимальной плитке и таблице кампании")
    void calculatesRewardForEveryTileLevel(int cashback, String expectedReward) {
        var result = calculator.calculate(stateWithMaxTile(cashback));

        assertThat(result.maxCashback()).isEqualTo(Cashback.of(cashback));
        assertThat(result.reward()).isEqualByComparingTo(new BigDecimal(expectedReward));
    }

    @ParameterizedTest(name = "Плитка {0}% даёт награду {1} при порогах 4% и 16%")
    @CsvSource({"2, 0", "8, 12.5", "32, 25"})
    @DisplayName("Выбирается наибольший достигнутый порог, включая значения вне диапазона таблицы")
    void usesHighestReachedThreshold(int cashback, String expectedReward) {
        var config = withRewards(
                List.of(
                        new RewardTier(Cashback.of(16), new BigDecimal("25.00")),
                        new RewardTier(Cashback.of(4), new BigDecimal("12.50"))),
                16);

        var result = new RewardCalculator(config).calculate(stateWithMaxTile(cashback));

        assertThat(result.maxCashback()).isEqualTo(Cashback.of(cashback));
        assertThat(result.reward()).isEqualByComparingTo(new BigDecimal(expectedReward));
    }

    @Test
    @DisplayName("Несколько максимальных плиток дают одну награду, исходное состояние и score сохраняются")
    void paysOnceAndPreservesStateAndScore() {
        var board = Board.empty(4)
                .withTile(new Position(0, 0), Cashback.of(16))
                .withTile(new Position(1, 1), Cashback.of(16))
                .withTile(new Position(3, 3), Cashback.of(8));
        var state = new GameState(board, 100, 20, 3_000_000_000L);

        var result = calculator.calculate(state);

        assertThat(result).isEqualTo(new GameResult(Cashback.of(16), new BigDecimal("500.00"), 3_000_000_000L));
        assertThat(state.board()).isSameAs(board);
        assertThat(state.board().tiles()).hasSize(3);
        assertThat(state.movesUsed()).isEqualTo(20);
        assertThat(state.score()).isEqualTo(3_000_000_000L);
    }

    @Test
    @DisplayName("Дробная награда сохраняет точность за пределами double")
    void preservesExactDecimalReward() {
        var amount = new BigDecimal("0.10000000000000000001");
        var config = withRewards(List.of(new RewardTier(Cashback.of(4), amount)), 4);

        var result = new RewardCalculator(config).calculate(stateWithMaxTile(4));

        assertThat(result.reward()).isEqualByComparingTo(amount);
    }

    @Test
    @DisplayName("Пустое поле отклоняется, поскольку у него нет максимальной плитки")
    void rejectsEmptyBoard() {
        var state = new GameState(Board.empty(4), 100, 0, 0);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> calculator.calculate(state))
                .withMessageContaining("пустого поля");
    }

    private static GameState stateWithMaxTile(int cashback) {
        var board = Board.empty(4)
                .withTile(new Position(0, 0), Cashback.of(cashback))
                .withTile(new Position(1, 1), Cashback.of(1));
        return new GameState(board, 100, 0, 0);
    }

    private static CampaignConfig withRewards(List<RewardTier> tiers, int maxRewardCashback) {
        return new CampaignConfig(
                BASE.boardSize(),
                BASE.movesLimit(),
                BASE.spawnRules(),
                tiers,
                Cashback.of(maxRewardCashback),
                BASE.maxRewardAllowedRate());
    }
}
