package academy.cashbackmerge.cli;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.Main;
import academy.cashbackmerge.support.CliRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Интерактивная игра и replay в CLI")
class PlayReplayCliTest {

    private static final Path EXAMPLE_CONFIG = Path.of("config", "campaign.example.json");
    private static final String SUMMARY_HEADER = "Результат партии:\n";
    private static final String MOVES_PREFIX = "Ходы для replay: ";

    @ParameterizedTest(name = "Seed {0}")
    @ValueSource(longs = {0, 42, -42})
    @DisplayName("Replay воспроизводит итог интерактивной игры с теми же seed и попытками")
    void replayMatchesInteractiveGame(long seed) {
        var played = run(
                " right \nleft\ndown\nUP\nQUIT\n",
                "play",
                "--config",
                EXAMPLE_CONFIG.toString(),
                "--seed",
                Long.toString(seed));
        var replayed = run(
                "",
                "replay",
                "--config",
                EXAMPLE_CONFIG.toString(),
                "--seed",
                Long.toString(seed),
                "--moves",
                " right ,left,down,UP ");

        assertThat(played.exitCode()).isZero();
        assertThat(replayed.exitCode()).isZero();
        assertThat(played.stderr()).isEmpty();
        assertThat(replayed.stderr()).isEmpty();
        assertThat(summary(played.stdout())).isEqualTo(replayed.stdout());
        assertThat(replayed.stdout()).contains("Ходы для replay: RIGHT,LEFT,DOWN,UP");
    }

    @Test
    @DisplayName("Ошибки ввода и пустые строки не становятся ходами, невалидные направления сохраняются для replay")
    void distinguishesUnknownInputFromUnchangedMove() {
        var played =
                run("\nwrong\n RiGhT \n LeFt \nquit\n", "play", "--config", EXAMPLE_CONFIG.toString(), "--seed", "42");
        var replayed =
                run("", "replay", "--config", EXAMPLE_CONFIG.toString(), "--seed", "42", "--moves", "RIGHT,LEFT");

        assertThat(played.exitCode()).isZero();
        assertThat(played.stderr()).contains("Неизвестное направление 'wrong'");
        assertThat(played.stdout())
                .contains("Ход не меняет поле.", "Прибавка к счёту: 0", "Ходы: 1 / 100")
                .doesNotContain("Ходы для replay: WRONG");
        assertThat(replayed.exitCode()).isZero();
        assertThat(replayed.stderr()).isEmpty();
        assertThat(summary(played.stdout())).isEqualTo(replayed.stdout());
    }

    @Test
    @DisplayName("Случайный seed интерактивной игры печатается и позволяет повторить её результат")
    void replaysGameWithGeneratedSeed() {
        var played = run("LEFT\nDOWN\nQUIT\n", "play", "--config", EXAMPLE_CONFIG.toString());

        assertThat(played.exitCode()).isZero();
        assertThat(played.stderr()).isEmpty();
        var seedLine = played.stdout().lines().findFirst().orElseThrow();
        assertThat(seedLine).matches("Seed: -?\\d+");
        long seed = Long.parseLong(seedLine.substring("Seed: ".length()));

        var replayed = run(
                "",
                "replay",
                "--config",
                EXAMPLE_CONFIG.toString(),
                "--seed",
                Long.toString(seed),
                "--moves",
                "LEFT,DOWN");

        assertThat(replayed.exitCode()).isZero();
        assertThat(replayed.stderr()).isEmpty();
        assertThat(summary(played.stdout())).isEqualTo(replayed.stdout());
    }

    @Test
    @DisplayName("Конец ввода останавливает игру без ошибки, пустой replay показывает то же начальное состояние")
    void handlesEndOfInputAndEmptyReplay() {
        var played = run("", "play", "--config", EXAMPLE_CONFIG.toString(), "--seed", "42");
        var replayed = run("", "replay", "--config", EXAMPLE_CONFIG.toString(), "--seed", "42");

        assertThat(played.exitCode()).isZero();
        assertThat(replayed.exitCode()).isZero();
        assertThat(played.stderr()).isEmpty();
        assertThat(replayed.stderr()).isEmpty();
        assertThat(summary(played.stdout())).isEqualTo(replayed.stdout());
        assertThat(replayed.stdout())
                .contains("Ходы для replay: (пустой список)", "Ходы: 0 / 100", "Партия не завершена.");
    }

    @ParameterizedTest(name = "Команда {0}")
    @ValueSource(strings = {"play", "replay"})
    @DisplayName("Справка новых команд доступна без обязательных аргументов")
    void showsHelpWithoutRequiredArguments(String command) {
        var result = run("", command, "--help");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("--config", "--seed");
        assertThat(result.stderr()).isEmpty();
    }

    @ParameterizedTest(name = "Неверный аргумент {1}, пример {index}")
    @MethodSource("invalidArguments")
    @DisplayName("Ошибки аргументов и последовательности ходов отклоняются до вывода партии")
    void rejectsInvalidArgumentsBeforeStarting(String[] arguments, String option) {
        var result = run("", arguments);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains(option).doesNotContain("\tat ", "Внутренняя ошибка приложения");
    }

    @ParameterizedTest(name = "Команда {0}")
    @ValueSource(strings = {"play", "replay"})
    @DisplayName("Ошибки конфигурации новых команд выводятся до создания поля")
    void rejectsInvalidConfiguration(String command, @TempDir Path directory) throws IOException {
        var path = directory.resolve("invalid campaign.json");
        writeConfig(path, 4, 0, false);

        var result = run("", command, "--config", path.toString(), "--seed", "42");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("movesLimit").doesNotContain("\tat ", "ConfigLoadException");
    }

    @ParameterizedTest(name = "Размер поля {0} x {0}")
    @ValueSource(ints = {4, 5})
    @DisplayName("Игра поддерживает путь с пробелами и размер поля, завершение по лимиту прекращает чтение")
    void stopsAtLimitAndReplaysConfiguredBoard(int size, @TempDir Path directory) throws IOException {
        var path = directory.resolve("campaign with spaces.json");
        writeConfig(path, size, 1, false);
        var played = run("UP\nLEFT\nDOWN\nRIGHT\nwrong\n", "play", "--config", path.toString(), "--seed", "42");

        assertThat(played.exitCode()).isZero();
        assertThat(played.stderr()).isEmpty();
        var finalOutput = summary(played.stdout());
        assertThat(finalOutput).contains("Ходы: 1 / 1", "Партия завершена: достигнут лимит ходов.");
        assertThat(finalOutput.lines().filter(line -> line.startsWith("|")).toList())
                .hasSize(size)
                .allSatisfy(row -> assertThat(
                                row.chars().filter(symbol -> symbol == '|').count())
                        .isEqualTo(size + 1));

        var moves = finalOutput
                .lines()
                .filter(line -> line.startsWith(MOVES_PREFIX))
                .findFirst()
                .orElseThrow()
                .substring(MOVES_PREFIX.length());
        var replayed = run("", "replay", "--config", path.toString(), "--seed", "42", "--moves", moves);

        assertThat(replayed.exitCode()).isZero();
        assertThat(replayed.stderr()).isEmpty();
        assertThat(replayed.stdout()).isEqualTo(finalOutput);
    }

    @Test
    @DisplayName("При тупике интерактивная игра завершается до чтения следующей команды")
    void stopsWhenNoMovesAreAvailable(@TempDir Path directory) throws IOException {
        var path = directory.resolve("blocked.json");
        writeConfig(path, 2, 100, true);

        var played = run("RIGHT\nUP\nwrong\n", "play", "--config", path.toString(), "--seed", "42");

        assertThat(played.exitCode()).isZero();
        assertThat(played.stderr()).isEmpty();
        assertThat(summary(played.stdout()))
                .contains("Ходы: 2 / 100", "Ходы для replay: RIGHT,UP", "Партия завершена: нет доступных ходов.");
    }

    @Test
    @DisplayName("Приложение отдельным процессом читает стандартный ввод и сохраняет данные replay")
    void readsStandardInputInApplicationProcess() {
        var result = CliRunner.runWithInput(
                "RIGHT\nLEFT\nQUIT\n", "play", "--config", EXAMPLE_CONFIG.toString(), "--seed", "42");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).contains("Ходы для replay: RIGHT,LEFT", "Ходы: 1 / 100");
    }

    private static Stream<Arguments> invalidArguments() {
        var config = EXAMPLE_CONFIG.toString();
        return Stream.of(
                Arguments.of(new String[] {"play"}, "--config"),
                Arguments.of(new String[] {"replay", "--config", config}, "--seed"),
                Arguments.of(new String[] {"replay", "--seed", "42"}, "--config"),
                Arguments.of(new String[] {"replay", "--config", config, "--seed", "wrong"}, "--seed"),
                Arguments.of(
                        new String[] {"replay", "--config", config, "--seed", "42", "--moves", "LEFT,WRONG"},
                        "--moves"),
                Arguments.of(
                        new String[] {"replay", "--config", config, "--seed", "42", "--moves", "LEFT,,UP"}, "--moves"),
                Arguments.of(
                        new String[] {"replay", "--config", config, "--seed", "42", "--moves", "LEFT,"}, "--moves"),
                Arguments.of(new String[] {"replay", "--config", config, "--seed", "42", "--moves", ""}, "--moves"),
                Arguments.of(new String[] {"replay", "--config", config, "--seed", "42", "--moves"}, "--moves"));
    }

    private static void writeConfig(Path path, int size, int movesLimit, boolean spawnMaxTile) throws IOException {
        var root = (ObjectNode) new ObjectMapper().readTree(EXAMPLE_CONFIG.toFile());
        root.put("boardSize", size);
        root.put("movesLimit", movesLimit);
        if (spawnMaxTile) {
            var rules = root.putArray("spawnRules");
            rules.addObject().put("cashback", "32").put("probability", "1");
        }
        Files.writeString(path, root.toString());
    }

    private static String summary(String output) {
        int index = output.lastIndexOf(SUMMARY_HEADER);
        assertThat(index).as("Итоговый блок партии").isNotNegative();
        return output.substring(index + SUMMARY_HEADER.length());
    }

    private static Result run(String input, String... arguments) {
        var stdout = new StringWriter();
        var stderr = new StringWriter();
        int exitCode;

        try (var reader = new StringReader(input);
                var out = new PrintWriter(stdout, true);
                var err = new PrintWriter(stderr, true)) {
            exitCode = Main.createCommandLine(reader).setOut(out).setErr(err).execute(arguments);
        }

        return new Result(
                exitCode,
                stdout.toString().replace("\r\n", "\n"),
                stderr.toString().replace("\r\n", "\n"));
    }

    private record Result(int exitCode, String stdout, String stderr) {}
}
