package academy.cashbackmerge;

import academy.cashbackmerge.cli.CashbackMergeCommand;
import academy.cashbackmerge.cli.InitializeCommand;
import academy.cashbackmerge.cli.PlayCommand;
import academy.cashbackmerge.cli.ReplayCommand;
import academy.cashbackmerge.config.ConfigLoadException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** Точка входа в Cashback Merge. */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {}

    public static void main(String[] args) {
        LOG.debug("Аргументы запуска: {}", String.join(" ", args));
        int exitCode;

        try (var input = new InputStreamReader(System.in, StandardCharsets.UTF_8)) {
            exitCode = createCommandLine(input).execute(args);
        } catch (IOException exception) {
            LOG.error("Не удалось закрыть стандартный ввод", exception);
            System.err.println("Ошибка завершения работы приложения. Подробности в журнале.");
            exitCode = CommandLine.ExitCode.SOFTWARE;
        }

        System.exit(exitCode);
    }

    /** Создание CLI с общей обработкой ошибок выполнения. */
    public static CommandLine createCommandLine(Reader input) {
        return new CommandLine(new CashbackMergeCommand())
                .addSubcommand(new InitializeCommand())
                .addSubcommand(new PlayCommand(input))
                .addSubcommand(new ReplayCommand())
                .setExecutionExceptionHandler((exception, commandLine, parseResult) -> {
                    if (exception instanceof ConfigLoadException) {
                        commandLine.getErr().println("Ошибка: " + exception.getMessage());
                        commandLine.getErr().flush();
                        return CommandLine.ExitCode.USAGE;
                    }

                    LOG.error("Неожиданная ошибка выполнения команды", exception);
                    commandLine.getErr().println("Внутренняя ошибка приложения. Подробности в журнале.");
                    commandLine.getErr().flush();
                    return CommandLine.ExitCode.SOFTWARE;
                });
    }
}
