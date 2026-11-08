package academy.cashbackmerge.cli;

import academy.cashbackmerge.config.CampaignConfigLoader;
import academy.cashbackmerge.presentation.ReportFileWriter;
import academy.cashbackmerge.presentation.ReportFormat;
import academy.cashbackmerge.presentation.SimulationReportFormatter;
import academy.cashbackmerge.simulation.SimulationRunner;
import academy.cashbackmerge.strategy.PlayerStrategy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/** Запуск симуляции кампании и сохранение отчёта. */
@Command(
        name = "simulate",
        mixinStandardHelpOptions = true,
        version = CashbackMergeCommand.VERSION,
        description = "Проверить кампанию серией партий и сохранить отчёт Markdown или HTML.")
public final class SimulateCommand implements Callable<Integer> {

    @Option(
            names = "--config",
            required = true,
            paramLabel = "<file>",
            description = "Путь к JSON-конфигурации кампании.")
    private @Nullable Path configPath;

    @Option(names = "--players", required = true, paramLabel = "<int>", description = "Положительное число игроков.")
    private int players;

    @Option(
            names = "--seed",
            paramLabel = "<long>",
            description = "Общий seed; при отсутствии выбирается случайно и указывается в консоли и отчёте.")
    private @Nullable Long seed;

    @Option(
            names = "--strategy",
            defaultValue = "random",
            converter = PlayerStrategyConverter.class,
            paramLabel = "<random|greedy-score>",
            description = "Стратегия игрока (по умолчанию: ${DEFAULT-VALUE}).")
    private PlayerStrategy strategy = PlayerStrategy.RANDOM;

    @Option(
            names = "--output",
            required = true,
            paramLabel = "<file.md|file.html>",
            description = "Путь отчёта; каталог должен существовать, существующий отчёт заменяется.")
    private @Nullable Path outputPath;

    @Spec
    private @Nullable CommandSpec spec;

    @Override
    public Integer call() throws IOException {
        var commandLine =
                Objects.requireNonNull(spec, "Команда не инициализирована").commandLine();
        if (players <= 0) {
            throw new ParameterException(commandLine, "--players должен быть положительным: " + players);
        }
        var source = Objects.requireNonNull(configPath, "Путь к конфигурации не задан")
                .toAbsolutePath()
                .normalize();
        var target = Objects.requireNonNull(outputPath, "Путь отчёта не задан")
                .toAbsolutePath()
                .normalize();

        ReportFormat format;
        try {
            format = ReportFormat.fromPath(target);
        } catch (IllegalArgumentException exception) {
            throw new ParameterException(commandLine, exception.getMessage(), exception);
        }

        var config = new CampaignConfigLoader().load(source);
        if (source.equals(target) || (Files.exists(target) && Files.isSameFile(source, target))) {
            throw new ParameterException(commandLine, "--output должен отличаться от файла конфигурации.");
        }

        var requestedSeed = seed;
        long actualSeed = requestedSeed == null ? new Random().nextLong() : requestedSeed;
        var report = new SimulationRunner(config).simulate(players, strategy, actualSeed);
        var content = new SimulationReportFormatter().format(report, format);

        try {
            new ReportFileWriter().write(target, content);
        } catch (IOException exception) {
            commandLine.getErr().println("Не удалось сохранить отчёт '" + target + "': " + exception.getMessage());
            commandLine.getErr().flush();
            return ExitCode.SOFTWARE;
        }

        commandLine
                .getOut()
                .print("Seed: " + report.seed() + "\nОтчёт сохранён: " + target + "\nСтатус кампании: "
                        + report.status() + "\n");
        commandLine.getOut().flush();
        return ExitCode.OK;
    }
}
