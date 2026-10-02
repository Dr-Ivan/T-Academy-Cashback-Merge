package academy.cashbackmerge;

import academy.cashbackmerge.cli.CashbackMergeCommand;
import academy.cashbackmerge.config.ConfigLoadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** Точка входа в Cashback Merge. */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {}

    public static void main(String[] args) {
        LOG.debug("Аргументы запуска: {}", String.join(" ", args));
        System.exit(createCommandLine().execute(args));
    }

    /** Создание CLI с общей обработкой ошибок выполнения. */
    public static CommandLine createCommandLine() {
        return new CommandLine(new CashbackMergeCommand())
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
