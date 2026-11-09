package academy.cashbackmerge.cli;

import academy.cashbackmerge.config.CampaignConfigLoader;
import academy.cashbackmerge.engine.GameSession;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.presentation.GameStateFormatter;
import academy.cashbackmerge.presentation.GameSummaryFormatter;
import academy.cashbackmerge.reward.RewardCalculator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.TypeConversionException;

/** Интерактивная партия с выводом последовательности попыток для replay. */
@Command(
        name = "play",
        mixinStandardHelpOptions = true,
        version = CashbackMergeCommand.VERSION,
        description = "Играть в Cashback Merge; направления вводятся по одному на строку.")
public final class PlayCommand implements Callable<Integer> {

    private final BufferedReader input;
    private final GameStateFormatter formatter = new GameStateFormatter();

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

    public PlayCommand(Reader input) {
        this.input = new BufferedReader(Objects.requireNonNull(input, "Источник ввода не должен быть null"));
    }

    @Override
    public Integer call() throws IOException {
        var path = Objects.requireNonNull(configPath, "Путь к конфигурации не задан");
        var config = new CampaignConfigLoader().load(path);
        var requestedSeed = seed;
        long actualSeed = requestedSeed == null ? new Random().nextLong() : requestedSeed;
        var session = new GameSession(config, actualSeed);
        var commandLine =
                Objects.requireNonNull(spec, "Команда не инициализирована").commandLine();
        var output = commandLine.getOut();
        var history = new ArrayList<Move>();

        output.print("Seed: " + session.seed() + "\nУправление: UP, DOWN, LEFT, RIGHT; QUIT - остановить игру.\n");
        output.print(formatter.format(session.state()));
        output.flush();

        play(session, history, output, commandLine.getErr());

        output.print("\nРезультат партии:\n");
        var result = new RewardCalculator(config).calculate(session.state());
        output.print(
                new GameSummaryFormatter().format(session.seed(), session.state(), session.status(), history, result));
        output.flush();
        return ExitCode.OK;
    }

    private void play(GameSession session, List<Move> history, PrintWriter output, PrintWriter error)
            throws IOException {
        var converter = new MoveConverter();

        while (!session.status().isFinished()) {
            output.print("Ход: ");
            output.flush();
            var line = input.readLine();
            if (line == null || "QUIT".equalsIgnoreCase(line.strip())) {
                return;
            }
            if (line.isBlank()) {
                continue;
            }

            Move move;
            try {
                move = converter.convert(line);
            } catch (TypeConversionException exception) {
                error.println(exception.getMessage());
                error.flush();
                continue;
            }

            var result = session.applyMove(move);
            history.add(move);
            if (result.changed()) {
                output.print("Прибавка к счёту: " + result.scoreDelta() + "\n");
                output.print(formatter.format(result.state()));
            } else {
                output.print("Ход не меняет поле.\n");
            }
            output.flush();
        }
    }
}
