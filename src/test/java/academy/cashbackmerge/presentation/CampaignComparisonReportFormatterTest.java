package academy.cashbackmerge.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.RewardTier;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.simulation.CampaignComparisonReport;
import academy.cashbackmerge.simulation.CampaignComparisonRunner;
import academy.cashbackmerge.simulation.CampaignResult;
import academy.cashbackmerge.simulation.SimulationReport;
import academy.cashbackmerge.strategy.PlayerStrategy;
import academy.cashbackmerge.support.CampaignFixtures;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Общий отчёт сравнения кампаний в Markdown и HTML")
class CampaignComparisonReportFormatterTest {

    private static final String EXPECTED_SUMMARY = """
            ## Сводное сравнение

            | Показатель | A | B |
            | --- | --- | --- |
            | Размер поля | 2 x 2 | 3 x 3 |
            | Лимит ходов | 8 | 5 |
            | Суммарный score | 42 | 80 |
            | Средний score | 10.5 | 20 |
            | Максимальный score | 20 | 30 |
            | Порог максимальной награды | 8% | 8% |
            | Максимальная награда | 10 | 10 |
            | Игроков с максимальной наградой | 1 | 2 |
            | Доля максимальных наград | 0.25 | 0.5 |
            | Допустимая доля | 0.25 | 0.2 |
            | Статус | OK | FAILED |

            """;

    private final CampaignComparisonReportFormatter formatter = new CampaignComparisonReportFormatter();

    @Test
    @DisplayName("Markdown сопоставляет показатели кампаний и содержит их полные результаты")
    void formatsMarkdownComparison() {
        var output = formatter.format(comparison("A", "B"), ReportFormat.MARKDOWN);

        assertThat(output)
                .startsWith("# Сравнение кампаний Cashback Merge\n\n")
                .contains(
                        "| Число игроков | 4 |",
                        "| Seed | -42 |",
                        "| Стратегия | greedy-score |",
                        EXPECTED_SUMMARY,
                        "## Кампания: A\n\n### Параметры запуска",
                        "## Кампания: B\n\n### Параметры запуска",
                        "| 4% | 2 |",
                        "| 8% | 2 |",
                        "| 2.5 | 2 |",
                        "| 10 | 2 |",
                        "| 1% | 0.7 |",
                        "| 8% | 10 |",
                        "одинаковые начальные seed игры и стратегии",
                        "Эмпирическая проверка");
        assertThat(output.lines()
                        .filter(line -> line.equals("### Распределение наград"))
                        .count())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("HTML содержит один документ, сводную таблицу и вложенные разделы каждой кампании")
    void formatsStandaloneHtmlComparison() {
        var output = formatter.format(comparison("A", "B"), ReportFormat.HTML);

        assertThat(output)
                .startsWith("<!DOCTYPE html>\n<html lang=\"ru\">\n")
                .contains(
                        "<title>Сравнение кампаний Cashback Merge</title>",
                        "<h1>Сравнение кампаний Cashback Merge</h1>",
                        "<h2>Общие параметры запуска</h2>",
                        "<th scope=\"col\">Показатель</th><th scope=\"col\">A</th><th scope=\"col\">B</th>",
                        "<tr><td>Средний score</td><td>10.5</td><td>20</td></tr>",
                        "<tr><td>Статус</td><td>OK</td><td>FAILED</td></tr>",
                        "<h2>Кампания: A</h2>\n<h3>Параметры запуска</h3>",
                        "<h2>Кампания: B</h2>\n<h3>Параметры запуска</h3>",
                        "<h3>Распределение максимальных плиток</h3>",
                        "<h3>Распределение наград</h3>",
                        "<h3>Правила появления плиток</h3>",
                        "<h3>Таблица наград</h3>",
                        "<tr><td>8%</td><td>2</td></tr>",
                        "<tr><td>10</td><td>2</td></tr>")
                .endsWith("</html>\n");
        assertThat(output.lines().filter(line -> line.equals("<!DOCTYPE html>")).count())
                .isEqualTo(1);
        assertThat(output.lines().filter(line -> line.equals("<table>")).count())
                .isEqualTo(14);
    }

    @Test
    @DisplayName("Названия кампаний не добавляют HTML-разметку в заголовки и ячейки")
    void escapesHtmlCampaignNames() {
        var name = "<script>alert(\"x&y\")</script>'";
        var escaped = "&lt;script&gt;alert(&quot;x&amp;y&quot;)&lt;/script&gt;&#39;";

        assertThat(formatter.format(comparison(name, "B"), ReportFormat.HTML))
                .contains("<th scope=\"col\">" + escaped + "</th>", "<h2>Кампания: " + escaped + "</h2>")
                .doesNotContain("<script>");
    }

    @Test
    @DisplayName("Спецсимволы в названии кампании не разрушают Markdown")
    void escapesMarkdownCampaignNames() {
        var name = "A|B\r\n[C]*_\\<&>";
        var escaped = "A\\|B<br>\\[C\\]\\*\\_\\\\&lt;&amp;&gt;";

        assertThat(formatter.format(comparison(name, "B"), ReportFormat.MARKDOWN))
                .contains("| Показатель | " + escaped + " | B |", "## Кампания: " + escaped + "\n\n");
    }

    @ParameterizedTest(name = "Формат {0}")
    @EnumSource(ReportFormat.class)
    @DisplayName("Общий отчёт воспроизводится для одинаковых данных при обеих стратегиях")
    void reproducesComparison(ReportFormat format) {
        var base = CampaignFixtures.simulationConfig();
        var configs = Map.of("B", otherConfig(), "A", base);
        var runner = new CampaignComparisonRunner();

        for (var strategy : PlayerStrategy.values()) {
            var first = formatter.format(runner.compare(configs, 8, strategy, -42), format);
            var repeated = formatter.format(runner.compare(configs, 8, strategy, -42), format);

            assertThat(repeated).as("Стратегия %s", strategy).isEqualTo(first);
        }
    }

    @ParameterizedTest(name = "Формат {0}")
    @EnumSource(ReportFormat.class)
    @DisplayName("Сводная таблица сохраняет точность наград и точный статус при округлении доли")
    void preservesPrecisionAndExactStatuses(ReportFormat format) {
        var amount = new BigDecimal("12345678901234567890.123456789");
        var first = precisionReport(amount, "0.3333333");
        var second = precisionReport(amount, "0.3333334");
        var comparison =
                new CampaignComparisonReport(List.of(new CampaignResult("A", first), new CampaignResult("B", second)));

        assertThat(formatter.format(comparison, format))
                .contains("12345678901234567890.123456789", "0.333333", "0.3333333", "0.3333334", "FAILED", "OK")
                .doesNotContain("E+");
    }

    private static CampaignComparisonReport comparison(String firstName, String secondName) {
        var first = new SimulationReport(
                CampaignFixtures.simulationConfig(),
                4,
                -42,
                PlayerStrategy.GREEDY_SCORE,
                Map.of(Cashback.of(8), 1, Cashback.of(2), 1, Cashback.of(4), 2),
                Map.of(BigDecimal.TEN, 1, BigDecimal.ZERO, 1, new BigDecimal("2.50"), 2),
                BigInteger.valueOf(42),
                20);
        var second = new SimulationReport(
                otherConfig(),
                4,
                -42,
                PlayerStrategy.GREEDY_SCORE,
                Map.of(Cashback.of(8), 2, Cashback.of(4), 2),
                Map.of(BigDecimal.TEN, 2, new BigDecimal("2.50"), 2),
                BigInteger.valueOf(80),
                30);
        return new CampaignComparisonReport(
                List.of(new CampaignResult(firstName, first), new CampaignResult(secondName, second)));
    }

    private static CampaignConfig otherConfig() {
        var base = CampaignFixtures.simulationConfig();
        return new CampaignConfig(
                3, 5, base.spawnRules(), base.rewardTable(), base.maxRewardCashback(), new BigDecimal("0.20"));
    }

    private static SimulationReport precisionReport(BigDecimal amount, String allowedRate) {
        var base = CampaignFixtures.simulationConfig();
        var config = new CampaignConfig(
                base.boardSize(),
                base.movesLimit(),
                base.spawnRules(),
                List.of(new RewardTier(Cashback.of(8), amount)),
                Cashback.of(8),
                new BigDecimal(allowedRate));
        return new SimulationReport(
                config,
                3,
                42,
                PlayerStrategy.RANDOM,
                Map.of(Cashback.of(8), 1, Cashback.of(2), 2),
                Map.of(amount, 1, BigDecimal.ZERO, 2),
                BigInteger.TWO,
                2);
    }
}
