package academy.cashbackmerge.cli;

import academy.cashbackmerge.config.CampaignConfigLoader;
import academy.cashbackmerge.engine.GameInitializer;
import academy.cashbackmerge.presentation.GameStateFormatter;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Загрузка кампании и вывод начального состояния партии. */
@Command(
        name = "init",
        mixinStandardHelpOptions = true,
        version = CashbackMergeCommand.VERSION,
        description = "Создать и показать начальное поле по конфигу и seed.")
public final class InitializeCommand implements Callable<Integer> {

    @Option(
            names = "--config",
            required = true,
            paramLabel = "<file>",
            description = "Путь к JSON-конфигурации кампании.")
    private @Nullable Path configPath;

    @Option(
            names = "--seed",
            paramLabel = "<long>",
            description = "Seed партии; при отсутствии выбирается случайно и выводится в консоль.")
    private @Nullable Long seed;

    @Spec
    private @Nullable CommandSpec spec;

    @Override
    public Integer call() {
        var path = Objects.requireNonNull(configPath, "Путь к конфигурации не задан");
        var config = new CampaignConfigLoader().load(path);

        var requestedSeed = seed;
        long actualSeed = requestedSeed == null ? new Random().nextLong() : requestedSeed;
        var state = new GameInitializer(config).initialize(actualSeed);
        var commandLine =
                Objects.requireNonNull(spec, "Команда не инициализирована").commandLine();

        var output = commandLine.getOut();
        output.print("Seed: " + actualSeed + "\n");
        output.print(new GameStateFormatter().format(state));
        output.flush();

        return ExitCode.OK;
    }
}
