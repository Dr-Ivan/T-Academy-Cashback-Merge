package academy.cashbackmerge.support;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.RewardTier;
import academy.cashbackmerge.config.SpawnRule;
import academy.cashbackmerge.model.Cashback;
import java.math.BigDecimal;
import java.util.List;

public final class CampaignFixtures {

    private CampaignFixtures() {}

    public static CampaignConfig validConfig() {
        return new CampaignConfig(
                4,
                100,
                List.of(
                        new SpawnRule(Cashback.of(1), new BigDecimal("0.85")),
                        new SpawnRule(Cashback.of(2), new BigDecimal("0.15"))),
                List.of(
                        new RewardTier(Cashback.of(2), BigDecimal.ZERO),
                        new RewardTier(Cashback.of(4), new BigDecimal("50")),
                        new RewardTier(Cashback.of(8), new BigDecimal("150")),
                        new RewardTier(Cashback.of(16), new BigDecimal("500")),
                        new RewardTier(Cashback.of(32), new BigDecimal("2000"))),
                Cashback.of(32),
                new BigDecimal("0.20"));
    }
}
