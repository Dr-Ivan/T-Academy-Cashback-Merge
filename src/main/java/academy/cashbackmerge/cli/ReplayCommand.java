package academy.cashbackmerge.cli;

import academy.cashbackmerge.config.CampaignConfigLoader;
import academy.cashbackmerge.engine.GameSession;
import academy.cashbackmerge.presentation.GameSummaryFormatter;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Воспроизведение партии по конфигу, seed и списку попыток ходов. */
@Command(
        name = "replay",
        mixinStandardHelpOptions = true,
        version = CashbackMergeCommand.VERSION,
        description = "Воспроизвести партию или её фрагмент по seed и списку ходов.")
public final class ReplayCommand implements Callable<Integer> {

    @Option(
            names = "--config",
            required = true,
            paramLabel = "<file>",
            description = "Путь к JSON-конфигурации кампании.")
    private @Nullable Path configPath;

    @Option(names = "--seed", required = true, paramLabel = "<long>", description = "Seed воспроизводимой партии.")
    private long seed;

    @Option(
            names = "--moves",
            converter = MoveSequence.Converter.class,
            paramLabel = "<UP,LEFT,...>",
            description = "Попытки ходов через запятую; при отсутствии показывается начальное состояние.")
    private MoveSequence sequence = new MoveSequence(List.of());

    @Spec
    private @Nullable CommandSpec spec;

    @Override
    public Integer call() {
        var path = Objects.requireNonNull(configPath, "Путь к конфигурации не задан");
        var config = new CampaignConfigLoader().load(path);
        var session = GameSession.replay(config, seed, sequence.moves());
        var output = Objects.requireNonNull(spec, "Команда не инициализирована")
                .commandLine()
                .getOut();

        output.print(
                new GameSummaryFormatter().format(session.seed(), session.state(), session.status(), sequence.moves()));
        output.flush();
        return ExitCode.OK;
    }
}
