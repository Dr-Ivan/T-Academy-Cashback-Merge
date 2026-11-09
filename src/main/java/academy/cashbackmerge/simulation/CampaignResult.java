package academy.cashbackmerge.simulation;

import java.util.Objects;

/** Именованный результат симуляции одной кампании. */
public record CampaignResult(String name, SimulationReport report) {

    public CampaignResult {
        Objects.requireNonNull(name, "Имя кампании не должно быть null");
        Objects.requireNonNull(report, "Отчёт кампании не должен быть null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Имя кампании не должно быть пустым");
        }
    }
}
