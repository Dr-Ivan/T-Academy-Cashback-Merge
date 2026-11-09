package academy.cashbackmerge.mandatory.mr1;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.engine.GameInitializer;
import academy.cashbackmerge.simulation.SimulationRunner;
import academy.cashbackmerge.strategy.PlayerStrategy;
import academy.cashbackmerge.support.CampaignFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: воспроизводимость по seed. */
@DisplayName("MR1. Воспроизводимость по seed")
class SeedTest {

    @Test
    @DisplayName("Одинаковый конфиг и seed дают одинаковое начальное поле")
    void sameConfigAndSeedProduceSameInitialBoard() {
        var config = CampaignFixtures.validConfig();
        var initializer = new GameInitializer(config);

        var first = initializer.initialize(42L);
        var second = initializer.initialize(42L);

        assertThat(first.board()).isEqualTo(second.board());
    }

    @Test
    @DisplayName("Одинаковый конфиг, seed и стратегия дают одинаковый отчёт симуляции")
    void sameInputsProduceSameSimulationReport() {
        var config = CampaignFixtures.simulationConfig();
        var runner = new SimulationRunner(config);

        for (var strategy : PlayerStrategy.values()) {
            var first = runner.simulate(8, strategy, -42);
            runner.simulate(3, strategy, 7);
            var repeated = runner.simulate(8, strategy, -42);

            assertThat(repeated).as("Стратегия %s", strategy).isEqualTo(first);
        }
    }
}
