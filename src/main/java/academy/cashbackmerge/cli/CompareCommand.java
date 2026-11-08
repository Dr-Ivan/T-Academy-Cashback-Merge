package academy.cashbackmerge.cli;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.CampaignConfigLoader;
import academy.cashbackmerge.presentation.CampaignComparisonReportFormatter;
import academy.cashbackmerge.presentation.ReportFileWriter;
import academy.cashbackmerge.presentation.ReportFormat;
import academy.cashbackmerge.simulation.CampaignComparisonRunner;
import academy.cashbackmerge.strategy.PlayerStrategy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/** Сравнение нескольких кампаний с общими параметрами симуляции. */
@Command(
        name = "compare",
        mixinStandardHelpOptions = true,
        version = CashbackMergeCommand.VERSION,
        description = "Сравнить кампании и сохранить общий отчёт Markdown или HTML.")
public final class CompareCommand implements Callable<Integer> {

    @Option(
            names = "--config",
            required = true,
            arity = "1",
            paramLabel = "<file>",
            description = "Путь к JSON-конфигурации; повторите опцию для каждой кампании, минимум два файла.")
    private List<Path> configPaths = new ArrayList<>();

    @Option(
            names = "--players",
            required = true,
            paramLabel = "<int>",
            description = "Число игроков в каждой кампании.")
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
        if (configPaths.size() < 2) {
            throw new ParameterException(commandLine, "--config нужно указать минимум для двух разных файлов.");
        }
        var target = Objects.requireNonNull(outputPath, "Путь отчёта не задан")
                .toAbsolutePath()
                .normalize();

        ReportFormat format;
        try {
            format = ReportFormat.fromPath(target);
        } catch (IllegalArgumentException exception) {
            throw new ParameterException(commandLine, exception.getMessage(), exception);
        }

        var campaigns = loadCampaigns(commandLine, target);
        var requestedSeed = seed;
        long actualSeed = requestedSeed == null ? new Random().nextLong() : requestedSeed;
        var report = new CampaignComparisonRunner().compare(campaigns, players, strategy, actualSeed);
        var content = new CampaignComparisonReportFormatter().format(report, format);

        try {
            new ReportFileWriter().write(target, content);
        } catch (IOException exception) {
            commandLine.getErr().println("Не удалось сохранить отчёт '" + target + "': " + exception.getMessage());
            commandLine.getErr().flush();
            return ExitCode.SOFTWARE;
        }

        commandLine.getOut().print("Seed: " + report.seed() + "\nОтчёт сохранён: " + target + "\n");
        for (var campaign : report.campaigns()) {
            commandLine
                    .getOut()
                    .println("Статус кампании '" + campaign.name() + "': "
                            + campaign.report().status());
        }
        commandLine.getOut().flush();
        return ExitCode.OK;
    }

    private Map<String, CampaignConfig> loadCampaigns(CommandLine commandLine, Path target) throws IOException {
        var loader = new CampaignConfigLoader();
        var campaigns = new HashMap<String, CampaignConfig>();
        var sources = new ArrayList<Path>();
        for (var path : configPaths) {
            var source = path.toAbsolutePath().normalize();
            var config = loader.load(source);
            if (source.equals(target) || (Files.exists(target) && Files.isSameFile(source, target))) {
                throw new ParameterException(commandLine, "--output должен отличаться от всех файлов конфигурации.");
            }
            for (var previous : sources) {
                if (Files.isSameFile(previous, source)) {
                    throw new ParameterException(commandLine, "--config содержит один файл несколько раз: " + path);
                }
            }
            sources.add(source);
            campaigns.put(path.normalize().toString(), config);
        }
        return Map.copyOf(campaigns);
    }
}
