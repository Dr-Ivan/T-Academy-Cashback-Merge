package academy.cashbackmerge.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.RewardTier;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.simulation.SimulationReport;
import academy.cashbackmerge.simulation.SimulationRunner;
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

@DisplayName("Отчёты симуляции в Markdown и HTML")
class SimulationReportFormatterTest {

    private static final String EXPECTED_MARKDOWN = """
            # Симуляция Cashback Merge

            ## Параметры запуска

            | Параметр | Значение |
            | --- | --- |
            | Число игроков | 4 |
            | Размер поля | 2 x 2 |
            | Лимит ходов | 8 |
            | Seed | -42 |
            | Стратегия | greedy-score |

            ## Результаты

            | Показатель | Значение |
            | --- | --- |
            | Суммарный score | 42 |
            | Средний score | 10.5 |
            | Максимальный score | 20 |
            | Порог максимальной награды | 8% |
            | Максимальная награда | 10 |
            | Игроков с максимальной наградой | 1 |
            | Доля максимальных наград | 0.25 |
            | Допустимая доля | 0.25 |
            | Статус | OK |

            ## Распределение максимальных плиток

            | Кэшбэк | Игроков |
            | --- | --- |
            | 2% | 1 |
            | 4% | 2 |
            | 8% | 1 |

            ## Распределение наград

            | Награда | Игроков |
            | --- | --- |
            | 0 | 1 |
            | 2.5 | 2 |
            | 10 | 1 |

            ## Правила появления плиток

            | Кэшбэк | Вероятность |
            | --- | --- |
            | 1% | 0.7 |
            | 2% | 0.3 |

            ## Таблица наград

            | Порог кэшбэка | Награда |
            | --- | --- |
            | 2% | 0 |
            | 4% | 2.5 |
            | 8% | 10 |

            Доли указаны от 0 до 1. Статус рассчитан по точным счётчикам до округления.

            Эмпирическая проверка для указанных конфигурации, seed, числа игроков и стратегии.

            """;

    private final SimulationReportFormatter formatter = new SimulationReportFormatter();

    @Test
    @DisplayName("Markdown содержит все параметры, статистику, конфигурацию и отсортированные распределения")
    void formatsMarkdownReport() {
        assertThat(formatter.format(report(), ReportFormat.MARKDOWN)).isEqualTo(EXPECTED_MARKDOWN);
    }

    @Test
    @DisplayName("HTML представляет самостоятельный документ с таблицами и теми же результатами")
    void formatsStandaloneHtmlReport() {
        var output = formatter.format(report(), ReportFormat.HTML);

        assertThat(output)
                .startsWith("<!DOCTYPE html>\n<html lang=\"ru\">\n")
                .contains(
                        "<meta charset=\"UTF-8\">",
                        "<title>Симуляция Cashback Merge</title>",
                        "<h1>Симуляция Cashback Merge</h1>",
                        "<th scope=\"col\">Кэшбэк</th>",
                        "<tr><td>Число игроков</td><td>4</td></tr>",
                        "<tr><td>Размер поля</td><td>2 x 2</td></tr>",
                        "<tr><td>Лимит ходов</td><td>8</td></tr>",
                        "<tr><td>Seed</td><td>-42</td></tr>",
                        "<tr><td>Стратегия</td><td>greedy-score</td></tr>",
                        "<tr><td>Средний score</td><td>10.5</td></tr>",
                        "<tr><td>Максимальный score</td><td>20</td></tr>",
                        "<tr><td>Доля максимальных наград</td><td>0.25</td></tr>",
                        "<tr><td>Статус</td><td>OK</td></tr>",
                        "<tr><td>2%</td><td>1</td></tr>\n<tr><td>4%</td><td>2</td></tr>\n<tr><td>8%</td><td>1</td></tr>",
                        "<tr><td>0</td><td>1</td></tr>\n<tr><td>2.5</td><td>2</td></tr>\n<tr><td>10</td><td>1</td></tr>")
                .endsWith("</html>\n");
        assertThat(output.lines().filter(line -> line.equals("<table>")).count())
                .isEqualTo(6);
    }

    @Test
    @DisplayName("HTML экранирует текст заголовков, абзацев и ячеек")
    void escapesHtmlText() {
        var text = "<script>alert(\"x&y\")</script>'";
        var escaped = "&lt;script&gt;alert(&quot;x&amp;y&quot;)&lt;/script&gt;&#39;";
        var format = ReportFormat.HTML;

        assertThat(format.document(text, ""))
                .contains("<title>" + escaped + "</title>", "<h1>" + escaped + "</h1>")
                .doesNotContain("<script>");
        assertThat(format.heading(text)).isEqualTo("<h2>" + escaped + "</h2>\n");
        assertThat(format.paragraph(text)).isEqualTo("<p>" + escaped + "</p>\n");
        assertThat(format.table(List.of(text), List.of(List.of(text))))
                .contains("<th scope=\"col\">" + escaped + "</th>", "<td>" + escaped + "</td>")
                .doesNotContain("<script>");
    }

    @Test
    @DisplayName("Спецсимволы и переносы строк в Markdown не разрушают таблицу")
    void escapesMarkdownTableCells() {
        var output = ReportFormat.MARKDOWN.table(List.of("Заголовок"), List.of(List.of("A|B\r\n[C]*_`\\<&>")));

        assertThat(output)
                .contains("A\\|B<br>\\[C\\]\\*\\_\\`\\\\&lt;&amp;&gt;")
                .endsWith("\n\n");
        assertThat(output.lines().filter(line -> !line.isBlank()).count()).isEqualTo(3);
    }

    @ParameterizedTest(name = "Формат {0}")
    @EnumSource(ReportFormat.class)
    @DisplayName("Десятичная точность сохраняется, округлённая доля не меняет статус")
    void preservesDecimalsAndExactStatus(ReportFormat format) {
        var base = CampaignFixtures.simulationConfig();
        var amount = new BigDecimal("12345678901234567890.123456789");
        var config = new CampaignConfig(
                base.boardSize(),
                base.movesLimit(),
                base.spawnRules(),
                List.of(new RewardTier(Cashback.of(8), amount)),
                Cashback.of(8),
                new BigDecimal("0.3333333"));
        var report = new SimulationReport(
                config,
                3,
                42,
                PlayerStrategy.RANDOM,
                Map.of(Cashback.of(8), 1, Cashback.of(2), 2),
                Map.of(amount, 1, BigDecimal.ZERO, 2),
                BigInteger.TWO,
                2);

        assertThat(formatter.format(report, format))
                .contains("12345678901234567890.123456789", "0.666667", "0.333333", "0.3333333", "FAILED")
                .doesNotContain("E+");
    }

    @ParameterizedTest(name = "Формат {0}")
    @EnumSource(ReportFormat.class)
    @DisplayName("Одинаковые входные данные воспроизводят текст отчёта для обеих стратегий")
    void reproducesRenderedReports(ReportFormat format) {
        var runner = new SimulationRunner(CampaignFixtures.simulationConfig());

        for (var strategy : PlayerStrategy.values()) {
            var first = formatter.format(runner.simulate(8, strategy, 42), format);
            var repeated = formatter.format(runner.simulate(8, strategy, 42), format);

            assertThat(repeated).as("Стратегия %s", strategy).isEqualTo(first);
        }
    }

    private static SimulationReport report() {
        return new SimulationReport(
                CampaignFixtures.simulationConfig(),
                4,
                -42,
                PlayerStrategy.GREEDY_SCORE,
                Map.of(Cashback.of(8), 1, Cashback.of(2), 1, Cashback.of(4), 2),
                Map.of(BigDecimal.TEN, 1, BigDecimal.ZERO, 1, new BigDecimal("2.50"), 2),
                BigInteger.valueOf(42),
                20);
    }
}
