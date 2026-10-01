package academy.cashbackmerge.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.support.CampaignFixtures;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Дополнительные проверки конфигурации кампании")
class CampaignConfigTest {

    private static final CampaignConfig BASE = CampaignFixtures.validConfig();

    @ParameterizedTest(name = "Размер поля {0}")
    @ValueSource(ints = {2, 4, 5})
    @DisplayName("Конфигурация принимает разные размеры поля")
    void acceptsDifferentBoardSizes(int size) {
        var config = createConfig(size, BASE.spawnRules(), BASE.rewardTable(), BASE.maxRewardAllowedRate());
        assertThat(config.boardSize()).isEqualTo(size);
    }

    @ParameterizedTest(name = "Размер поля {0} отклоняется")
    @ValueSource(ints = {-1, 0, 1})
    @DisplayName("Конфигурация отклоняет слишком маленькое поле")
    void rejectsTooSmallBoard(int size) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> createConfig(size, BASE.spawnRules(), BASE.rewardTable(), BASE.maxRewardAllowedRate()))
                .withMessageContaining("boardSize");
    }

    @ParameterizedTest(name = "Вероятность {0} отклоняется")
    @ValueSource(strings = {"-0.01", "1.01"})
    @DisplayName("Вероятность появления вне диапазона [0, 1] отклоняется")
    void rejectsProbabilityOutsideRange(String probability) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SpawnRule(Cashback.of(1), new BigDecimal(probability)))
                .withMessageContaining("spawnRules.probability");
    }

    @Test
    @DisplayName("Повторяющиеся уровни появления плиток отклоняются")
    void rejectsDuplicateSpawnLevels() {
        var rules = List.of(
                new SpawnRule(Cashback.of(1), new BigDecimal("0.5")),
                new SpawnRule(Cashback.of(1), new BigDecimal("0.5")));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> createConfig(4, rules, BASE.rewardTable(), BASE.maxRewardAllowedRate()))
                .withMessageContaining("повторяющийся");
    }

    @Test
    @DisplayName("Отрицательная награда отклоняется")
    void rejectsNegativeReward() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RewardTier(Cashback.of(2), new BigDecimal("-0.01")))
                .withMessageContaining("rewardTable.reward");
    }

    @ParameterizedTest(name = "Невалидная таблица наград, пример {index}")
    @MethodSource("invalidRewardTables")
    @DisplayName("Нарушения структуры и порядка наград отклоняются")
    void rejectsInvalidRewardTable(List<RewardTier> tiers, String message) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createConfig(4, BASE.spawnRules(), tiers, BASE.maxRewardAllowedRate()))
                .withMessageContaining(message);
    }

    @ParameterizedTest(name = "Допустимая доля {0}")
    @ValueSource(strings = {"0", "1", "0.200"})
    @DisplayName("Допустимая доля максимальных наград включает границы диапазона")
    void acceptsAllowedRateWithinRange(String rate) {
        var config = createConfig(4, BASE.spawnRules(), BASE.rewardTable(), new BigDecimal(rate));
        assertThat(config.maxRewardAllowedRate()).isEqualByComparingTo(new BigDecimal(rate));
    }

    @ParameterizedTest(name = "Доля {0} отклоняется")
    @ValueSource(strings = {"-0.01", "1.01"})
    @DisplayName("Допустимая доля максимальных наград вне [0, 1] отклоняется")
    void rejectsAllowedRateOutsideRange(String rate) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createConfig(4, BASE.spawnRules(), BASE.rewardTable(), new BigDecimal(rate)))
                .withMessageContaining("maxRewardAllowedRate");
    }

    @Test
    @DisplayName("Таблица наград упорядочивается по уровню кэшбэка")
    void sortsRewardTableByCashback() {
        var config = createConfig(4, BASE.spawnRules(), BASE.rewardTable().reversed(), BASE.maxRewardAllowedRate());
        assertThat(config.rewardTable()).containsExactlyElementsOf(BASE.rewardTable());
    }

    @Test
    @DisplayName("Исходные списки и доступ к конфигу не позволяют изменить его содержимое")
    void protectsConfigurationLists() {
        var rules = new ArrayList<>(BASE.spawnRules());
        var tiers = new ArrayList<>(BASE.rewardTable());
        var config = createConfig(4, rules, tiers, BASE.maxRewardAllowedRate());

        rules.clear();
        tiers.clear();

        assertThat(config.spawnRules()).containsExactlyElementsOf(BASE.spawnRules());
        assertThat(config.rewardTable()).containsExactlyElementsOf(BASE.rewardTable());
        assertThatThrownBy(() -> config.spawnRules().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> config.rewardTable().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    private static Stream<Arguments> invalidRewardTables() {
        return Stream.of(
                Arguments.of(List.<RewardTier>of(), "rewardTable"),
                Arguments.of(List.of(tier(32, "100"), tier(32, "200")), "повторяющийся"),
                Arguments.of(List.of(tier(16, "600"), tier(32, "500")), "не должны уменьшаться"),
                Arguments.of(List.of(tier(16, "500"), tier(32, "500")), "строго выше"),
                Arguments.of(List.of(tier(16, "500")), "maxRewardCashback"));
    }

    private static RewardTier tier(int cashback, String reward) {
        return new RewardTier(Cashback.of(cashback), new BigDecimal(reward));
    }

    private static CampaignConfig createConfig(
            int size, List<SpawnRule> rules, List<RewardTier> tiers, BigDecimal allowedRate) {
        return new CampaignConfig(size, BASE.movesLimit(), rules, tiers, BASE.maxRewardCashback(), allowedRate);
    }
}
