package academy.cashbackmerge.presentation;

import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.simulation.SimulationReport;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SimulationReportFormatter {

    public String format(SimulationReport report, ReportFormat format) {
        Objects.requireNonNull(report, "Отчёт не должен быть null");
        Objects.requireNonNull(format, "Формат отчёта не должен быть null");

        var config = report.config();
        var output = format.heading("Параметры запуска")
                + format.table(List.of("Параметр", "Значение"), parameters(report))
                + format.heading("Результаты")
                + format.table(List.of("Показатель", "Значение"), summary(report))
                + format.heading("Распределение максимальных плиток")
                + format.table(List.of("Кэшбэк", "Игроков"), cashbacks(report))
                + format.heading("Распределение наград")
                + format.table(List.of("Награда", "Игроков"), rewards(report))
                + format.heading("Правила появления плиток")
                + format.table(
                        List.of("Кэшбэк", "Вероятность"),
                        config.spawnRules().stream()
                                .map(rule -> List.of(rule.cashback().value() + "%", number(rule.probability())))
                                .toList())
                + format.heading("Таблица наград")
                + format.table(
                        List.of("Порог кэшбэка", "Награда"),
                        config.rewardTable().stream()
                                .map(tier -> List.of(tier.cashback().value() + "%", number(tier.reward())))
                                .toList())
                + format.paragraph("Доли указаны от 0 до 1. Статус рассчитан по точным счётчикам до округления.")
                + format.paragraph(
                        "Эмпирическая проверка для указанных конфигурации, seed, числа игроков и стратегии.");

        return format.document("Симуляция Cashback Merge", output);
    }

    private static List<List<String>> parameters(SimulationReport report) {
        var config = report.config();
        return List.of(
                List.of("Число игроков", Integer.toString(report.players())),
                List.of("Размер поля", config.boardSize() + " x " + config.boardSize()),
                List.of("Лимит ходов", Integer.toString(config.movesLimit())),
                List.of("Seed", Long.toString(report.seed())),
                List.of("Стратегия", report.strategy().cliName()));
    }

    private static List<List<String>> summary(SimulationReport report) {
        var config = report.config();
        return List.of(
                List.of("Суммарный score", report.totalScore().toString()),
                List.of("Средний score", number(report.averageScore())),
                List.of("Максимальный score", Long.toString(report.maxScore())),
                List.of("Порог максимальной награды", config.maxRewardCashback().value() + "%"),
                List.of(
                        "Максимальная награда",
                        number(config.rewardTable().getLast().reward())),
                List.of("Игроков с максимальной наградой", Integer.toString(report.maxRewardPlayers())),
                List.of("Доля максимальных наград", number(report.actualMaxRewardRate())),
                List.of("Допустимая доля", number(config.maxRewardAllowedRate())),
                List.of("Статус", report.status().name()));
    }

    private static List<List<String>> cashbacks(SimulationReport report) {
        return report.maxCashbackDistribution().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(Cashback::value)))
                .map(entry ->
                        List.of(entry.getKey().value() + "%", entry.getValue().toString()))
                .toList();
    }

    private static List<List<String>> rewards(SimulationReport report) {
        return report.rewardDistribution().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> List.of(number(entry.getKey()), entry.getValue().toString()))
                .toList();
    }

    private static String number(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
