package academy.cashbackmerge.cli;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.Main;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Командная строка Cashback Merge")
class CashbackMergeCliTest {

    private static final Path EXAMPLE_CONFIG = Path.of("config", "campaign.example.json");

    @Test
    @DisplayName("Запуск без аргументов показывает справку и завершается успешно")
    void showsHelpWithoutArguments() {
        var result = run();

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("cashback-merge", "init");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    @DisplayName("Справка команды init доступна без обязательного конфига")
    void showsInitializationHelpWithoutConfig() {
        var result = run("init", "--help");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("--config", "--seed");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    @DisplayName("Одинаковые конфиг и seed дают одинаковый вывод с двумя начальными плитками")
    void initializesReproducibly() {
        var first = run("init", "--config", EXAMPLE_CONFIG.toString(), "--seed", "42");
        var second = run("init", "--config", EXAMPLE_CONFIG.toString(), "--seed", "42");

        assertThat(first.exitCode()).isZero();
        assertThat(second.exitCode()).isZero();
        assertThat(first.stderr()).isEmpty();
        assertThat(second.stderr()).isEmpty();
        assertThat(first.stdout()).isEqualTo(second.stdout()).startsWith("Seed: 42\nСчёт: 0\nХоды: 0 / 100\n");

        var tiles = Pattern.compile("\\d+%")
                .matcher(first.stdout())
                .results()
                .map(MatchResult::group)
                .toList();
        assertThat(tiles).hasSize(2).allMatch(tile -> "1%".equals(tile) || "2%".equals(tile));
    }

    @ParameterizedTest(name = "Ошибка в аргументе {1}")
    @MethodSource("invalidArguments")
    @DisplayName("Отсутствующий конфиг, неверный seed и неизвестная опция отклоняются")
    void rejectsInvalidArguments(String[] args, String expectedOption) {
        var result = run(args);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains(expectedOption);
    }

    @Test
    @DisplayName("Отсутствующий файл конфига даёт понятную ошибку без стека исключения")
    void rejectsMissingConfigFile(@TempDir Path directory) {
        var path = directory.resolve("missing.json");

        var result = run("init", "--config", path.toString());

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .contains("Не удалось прочитать конфигурацию", path.toString())
                .doesNotContain("ConfigLoadException", "\tat ");
    }

    @Test
    @DisplayName("Ошибки валидации конфига выводятся до создания поля")
    void rejectsInvalidCampaign(@TempDir Path directory) throws IOException {
        var path = directory.resolve("invalid.json");
        writeConfig(path, 4, 0);

        var result = run("init", "--config", path.toString());

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .contains("Некорректная конфигурация", "movesLimit")
                .doesNotContain("ConfigLoadException", "\tat ");
    }

    @Test
    @DisplayName("CLI поддерживает размер поля из конфига и путь с пробелами")
    void supportsConfiguredSizeAndPathWithSpaces(@TempDir Path directory) throws IOException {
        var path = directory.resolve("campaign with spaces.json");
        writeConfig(path, 5, 7);

        var result = run("init", "--config", path.toString(), "--seed", "42");
        var rows = result.stdout().lines().filter(line -> line.startsWith("|")).toList();

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).startsWith("Seed: 42\nСчёт: 0\nХоды: 0 / 7\n");
        assertThat(rows).hasSize(5);
        assertThat(rows)
                .allSatisfy(row -> assertThat(
                                row.chars().filter(symbol -> symbol == '|').count())
                        .isEqualTo(6));
    }

    @Test
    @DisplayName("Случайно выбранный seed печатается и позволяет повторить начальное состояние")
    void reproducesInitializationWithGeneratedSeed() {
        var first = run("init", "--config", EXAMPLE_CONFIG.toString());

        assertThat(first.exitCode()).isZero();
        assertThat(first.stderr()).isEmpty();

        var seedLine = first.stdout().lines().findFirst().orElseThrow();
        assertThat(seedLine).matches("Seed: -?\\d+");
        long generatedSeed = Long.parseLong(seedLine.substring("Seed: ".length()));

        var repeated = run("init", "--config", EXAMPLE_CONFIG.toString(), "--seed", Long.toString(generatedSeed));

        assertThat(repeated.exitCode()).isZero();
        assertThat(repeated.stderr()).isEmpty();
        assertThat(repeated.stdout()).isEqualTo(first.stdout());
    }

    private static Stream<Arguments> invalidArguments() {
        return Stream.of(
                Arguments.of(new String[] {"init"}, "--config"),
                Arguments.of(new String[] {"init", "--config", EXAMPLE_CONFIG.toString(), "--seed", "wrong"}, "--seed"),
                Arguments.of(new String[] {"init", "--config", EXAMPLE_CONFIG.toString(), "--unknown"}, "--unknown"));
    }

    private static void writeConfig(Path path, int size, int movesLimit) throws IOException {
        var root = (ObjectNode) new ObjectMapper().readTree(EXAMPLE_CONFIG.toFile());
        root.put("boardSize", size);
        root.put("movesLimit", movesLimit);
        Files.writeString(path, root.toString());
    }

    private static Result run(String... args) {
        var stdout = new StringWriter();
        var stderr = new StringWriter();
        int exitCode;

        try (var out = new PrintWriter(stdout, true);
                var err = new PrintWriter(stderr, true)) {
            exitCode = Main.createCommandLine().setOut(out).setErr(err).execute(args);
        }

        return new Result(
                exitCode,
                stdout.toString().replace("\r\n", "\n"),
                stderr.toString().replace("\r\n", "\n"));
    }

    private record Result(int exitCode, String stdout, String stderr) {}
}
