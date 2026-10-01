package academy.cashbackmerge.mandatory.mr1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.SpawnRule;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.support.CampaignFixtures;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: конфиг кампании. */
@DisplayName("MR1. Конфиг кампании")
class ConfigValidationTest {

    @Test
    @DisplayName("Валидный конфиг принимается")
    void validConfigIsAccepted() {
        var config = CampaignFixtures.validConfig();

        assertThat(config.boardSize()).isEqualTo(4);
        assertThat(config.movesLimit()).isEqualTo(100);
        assertThat(config.spawnRules()).hasSize(2);
        assertThat(config.rewardTable()).hasSize(5);
        assertThat(config.maxRewardCashback()).isEqualTo(Cashback.of(32));
        assertThat(config.maxRewardAllowedRate()).isEqualByComparingTo(new BigDecimal("0.20"));
    }

    @Test
    @DisplayName("Сумма вероятностей spawn-правил не равна 1 — конфиг отклоняется")
    void probabilitiesNotSummingToOneAreRejected() {
        for (var probability : List.of("0.84", "0.86", "0.84999999999999999999")) {
            var rules = List.of(
                    new SpawnRule(Cashback.of(1), new BigDecimal(probability)),
                    new SpawnRule(Cashback.of(2), new BigDecimal("0.15")));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> createConfig(100, rules))
                    .withMessageContaining("Сумма вероятностей");
        }
    }

    @Test
    @DisplayName("movesLimit <= 0 — конфиг отклоняется")
    void nonPositiveMovesLimitIsRejected() {
        var rules = CampaignFixtures.validConfig().spawnRules();

        for (int movesLimit : new int[] {0, -1}) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> createConfig(movesLimit, rules))
                    .withMessageContaining("movesLimit");
        }
    }

    @Test
    @DisplayName("Пустой список spawn-правил — конфиг отклоняется")
    void emptySpawnRulesAreRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createConfig(100, List.of()))
                .withMessageContaining("spawnRules");
    }

    private static CampaignConfig createConfig(int movesLimit, List<SpawnRule> rules) {
        var base = CampaignFixtures.validConfig();

        return new CampaignConfig(
                base.boardSize(),
                movesLimit,
                rules,
                base.rewardTable(),
                base.maxRewardCashback(),
                base.maxRewardAllowedRate());
    }
}
