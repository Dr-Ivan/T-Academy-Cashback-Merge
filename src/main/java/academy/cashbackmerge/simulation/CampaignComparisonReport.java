package academy.cashbackmerge.simulation;

import academy.cashbackmerge.strategy.PlayerStrategy;
import java.util.HashSet;
import java.util.List;

/** Результаты нескольких кампаний с одинаковыми параметрами симуляции. */
public record CampaignComparisonReport(List<CampaignResult> campaigns) {

    public CampaignComparisonReport {
        campaigns = List.copyOf(campaigns);
        if (campaigns.size() < 2) {
            throw new IllegalArgumentException("Для сравнения нужны как минимум две кампании");
        }

        var first = campaigns.getFirst().report();
        var names = new HashSet<String>();
        for (var campaign : campaigns) {
            if (!names.add(campaign.name())) {
                throw new IllegalArgumentException("Имя кампании повторяется: " + campaign.name());
            }
            var report = campaign.report();
            if (report.players() != first.players()
                    || report.seed() != first.seed()
                    || report.strategy() != first.strategy()) {
                throw new IllegalArgumentException("Кампании должны иметь одинаковые число игроков, seed и стратегию");
            }
        }
    }

    public int players() {
        return campaigns.getFirst().report().players();
    }

    public long seed() {
        return campaigns.getFirst().report().seed();
    }

    public PlayerStrategy strategy() {
        return campaigns.getFirst().report().strategy();
    }
}
