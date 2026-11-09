package academy.cashbackmerge.presentation;

import academy.cashbackmerge.simulation.CampaignComparisonReport;
import academy.cashbackmerge.simulation.SimulationReport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Сводное сравнение и подробные результаты кампаний в одном документе. */
public final class CampaignComparisonReportFormatter {

    private final SimulationReportFormatter simulationFormatter = new SimulationReportFormatter();

    public String format(CampaignComparisonReport report, ReportFormat format) {
        Objects.requireNonNull(report, "Отчёт сравнения не должен быть null");
        Objects.requireNonNull(format, "Формат отчёта не должен быть null");

        var headers = new ArrayList<String>();
        headers.add("Показатель");
        for (var campaign : report.campaigns()) {
            headers.add(campaign.name());
        }

        var output = new StringBuilder()
                .append(format.heading("Общие параметры запуска"))
                .append(format.table(
                        List.of("Параметр", "Значение"),
                        List.of(
                                List.of("Число игроков", Integer.toString(report.players())),
                                List.of("Seed", Long.toString(report.seed())),
                                List.of("Стратегия", report.strategy().cliName()))))
                .append(format.heading("Сводное сравнение"))
                .append(format.table(headers, summary(report)))
                .append(format.paragraph(
                        "Для каждого игрока во всех кампаниях используются одинаковые начальные seed игры и стратегии. "
                                + "Различия конфигураций могут приводить к разным партиям."));

        for (var campaign : report.campaigns()) {
            output.append(format.heading("Кампания: " + campaign.name()))
                    .append(simulationFormatter.formatDetails(campaign.report(), format));
        }

        return format.document("Сравнение кампаний Cashback Merge", output.toString());
    }

    private static List<List<String>> summary(CampaignComparisonReport report) {
        return List.of(
                row("Размер поля", report, result -> {
                    var size = result.config().boardSize();
                    return size + " x " + size;
                }),
                row(
                        "Лимит ходов",
                        report,
                        result -> Integer.toString(result.config().movesLimit())),
                row("Суммарный score", report, result -> result.totalScore().toString()),
                row("Средний score", report, result -> number(result.averageScore())),
                row("Максимальный score", report, result -> Long.toString(result.maxScore())),
                row(
                        "Порог максимальной награды",
                        report,
                        result -> result.config().maxRewardCashback().value() + "%"),
                row(
                        "Максимальная награда",
                        report,
                        result -> number(result.config().rewardTable().getLast().reward())),
                row("Игроков с максимальной наградой", report, result -> Integer.toString(result.maxRewardPlayers())),
                row("Доля максимальных наград", report, result -> number(result.actualMaxRewardRate())),
                row("Допустимая доля", report, result -> number(result.config().maxRewardAllowedRate())),
                row("Статус", report, result -> result.status().name()));
    }

    private static List<String> row(
            String label, CampaignComparisonReport report, Function<SimulationReport, String> value) {
        var cells = new ArrayList<String>();
        cells.add(label);
        for (var campaign : report.campaigns()) {
            cells.add(value.apply(campaign.report()));
        }
        return List.copyOf(cells);
    }

    private static String number(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
