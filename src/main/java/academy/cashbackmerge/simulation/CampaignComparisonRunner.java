package academy.cashbackmerge.simulation;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.strategy.PlayerStrategy;
import java.util.Map;
import java.util.Objects;

/** Сравнение кампаний при одинаковом числе игроков, стратегии и seed. */
public final class CampaignComparisonRunner {

    public CampaignComparisonReport compare(
            Map<String, CampaignConfig> campaigns, int players, PlayerStrategy strategy, long seed) {
        if (players <= 0) {
            throw new IllegalArgumentException("Число игроков должно быть положительным: " + players);
        }

        Objects.requireNonNull(strategy, "Стратегия не должна быть null");
        var configs = Map.copyOf(Objects.requireNonNull(campaigns, "Кампании не должны быть null"));
        if (configs.size() < 2) {
            throw new IllegalArgumentException("Для сравнения нужны как минимум две кампании");
        }
        for (var name : configs.keySet()) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("Имя кампании не должно быть пустым");
            }
        }

        var results = configs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new CampaignResult(
                        entry.getKey(), new SimulationRunner(entry.getValue()).simulate(players, strategy, seed)))
                .toList();
        return new CampaignComparisonReport(results);
    }
}
