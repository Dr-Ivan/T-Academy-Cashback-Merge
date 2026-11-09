package academy.cashbackmerge.engine;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.support.CampaignFixtures;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Создание начального состояния партии")
class GameInitializerTest {

    @ParameterizedTest(name = "Поле размером {0} x {0}")
    @ValueSource(ints = {2, 4, 5})
    @DisplayName("Начальная партия использует настройки и содержит две разрешённые плитки")
    void initializesGameFromConfiguration(int size) {
        var base = CampaignFixtures.validConfig();
        var config = new CampaignConfig(
                size, 7, base.spawnRules(), base.rewardTable(), base.maxRewardCashback(), base.maxRewardAllowedRate());

        var state = new GameInitializer(config).initialize(new Random(42L));

        assertThat(state.board().size()).isEqualTo(size);
        assertThat(state.board().tiles()).hasSize(2);
        assertThat(state.board().tiles().values())
                .allMatch(tile -> config.spawnRules().stream()
                        .anyMatch(rule -> rule.cashback().equals(tile)));
        assertThat(state.movesLimit()).isEqualTo(7);
        assertThat(state.movesUsed()).isZero();
        assertThat(state.score()).isZero();
    }
}
