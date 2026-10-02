package academy.cashbackmerge.cli;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Корневая команда и справка по доступным режимам. */
@Command(
        name = "cashback-merge",
        mixinStandardHelpOptions = true,
        version = CashbackMergeCommand.VERSION,
        description = "Игра Cashback Merge и проверка кампаний.",
        subcommands = InitializeCommand.class)
public final class CashbackMergeCommand implements Runnable {

    public static final String VERSION = "Cashback Merge MR1";

    @Spec
    private @Nullable CommandSpec spec;

    @Override
    public void run() {
        var commandLine =
                Objects.requireNonNull(spec, "Команда не инициализирована").commandLine();
        commandLine.usage(commandLine.getOut());
    }
}
