package academy.cashbackmerge.cli;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.Main;
import academy.cashbackmerge.config.CampaignConfigLoader;
import academy.cashbackmerge.presentation.CampaignComparisonReportFormatter;
import academy.cashbackmerge.presentation.ReportFormat;
import academy.cashbackmerge.simulation.CampaignComparisonRunner;
import academy.cashbackmerge.strategy.PlayerStrategy;
import academy.cashbackmerge.support.CliRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Сравнение кампаний через CLI")
class CompareCliTest {

    private static final Path EXAMPLE_CONFIG = Path.of("config", "campaign.example.json");

    @Test
    @DisplayName("Справка compare доступна без обязательных аргументов")
    void showsHelp() {
        var result = run("compare", "--help");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).contains("--config", "--players", "--seed", "--strategy", "--output");
    }

    @ParameterizedTest(name = "Стратегия {0}, формат {1}")
    @CsvSource({"random, md", "' GrEeDy-ScOrE ', HTML"})
    @DisplayName("CLI сохраняет общий отчёт в UTF-8 и различает одноимённые файлы из разных каталогов")
    void savesComparison(String strategyName, String extension, @TempDir Path directory) throws IOException {
        var first = directory.resolve("первая кампания").resolve("campaign.json");
        var second = directory.resolve("вторая кампания").resolve("campaign.json");
        var target = directory.resolve("общий отчёт." + extension);
        writeConfig(first, 2, false);
        writeConfig(second, 3, true);
        Files.writeString(target, "previous report", StandardCharsets.UTF_8);

        var result = run(arguments(first, second, target, strategyName));
        var loader = new CampaignConfigLoader();
        var report = new CampaignComparisonRunner()
                .compare(
                        Map.of(first.toString(), loader.load(first), second.toString(), loader.load(second)),
                        8,
                        PlayerStrategy.fromCliName(strategyName),
                        -42);
        var expected = new CampaignComparisonReportFormatter().format(report, ReportFormat.fromPath(target));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout())
                .startsWith("Seed: -42\n")
                .contains(
                        "Отчёт сохранён: " + target,
                        "Статус кампании '" + first + "': OK",
                        "Статус кампании '" + second + "': FAILED");
        assertThat(Files.readAllBytes(target)).isEqualTo(expected.getBytes(StandardCharsets.UTF_8));

        var reversed = run(arguments(second, first, target, strategyName));

        assertThat(reversed.exitCode()).isZero();
        assertThat(reversed.stderr()).isEmpty();
        assertThat(reversed.stdout()).isEqualTo(result.stdout());
        assertThat(Files.readAllBytes(target)).isEqualTo(expected.getBytes(StandardCharsets.UTF_8));
        try (var files = Files.walk(directory)) {
            assertThat(files.filter(Files::isRegularFile).toList()).containsExactlyInAnyOrder(first, second, target);
        }
    }

    @Test
    @DisplayName("При отсутствии strategy сравнение использует random")
    void defaultsToRandomStrategy(@TempDir Path directory) throws IOException {
        var first = directory.resolve("a.json");
        var second = directory.resolve("b.json");
        var implicitTarget = directory.resolve("implicit.md");
        var explicitTarget = directory.resolve("explicit.md");
        writeConfig(first, 2, false);
        writeConfig(second, 3, false);
        var args = new ArrayList<>(List.of(arguments(first, second, implicitTarget, "random")));
        removeOption(args, "--strategy");

        var implicit = run(args.toArray(String[]::new));
        var explicit = run(arguments(first, second, explicitTarget, "random"));

        assertThat(implicit.exitCode()).isZero();
        assertThat(explicit.exitCode()).isZero();
        assertThat(implicit.stderr()).isEmpty();
        assertThat(explicit.stderr()).isEmpty();
        assertThat(Files.readAllBytes(implicitTarget)).isEqualTo(Files.readAllBytes(explicitTarget));
    }

    @ParameterizedTest(name = "Стратегия {0}, формат {1}")
    @CsvSource({"random, md", "greedy-score, html"})
    @DisplayName("Без seed выбирается одно общее значение, которое воспроизводит весь отчёт")
    void generatesReplayableSharedSeed(String strategy, String extension, @TempDir Path directory) throws IOException {
        var first = directory.resolve("a.json");
        var second = directory.resolve("b.json");
        var target = directory.resolve("generated." + extension);
        var repeatedTarget = directory.resolve("repeated." + extension);
        writeConfig(first, 2, false);
        writeConfig(second, 3, false);
        var args = new ArrayList<>(List.of(arguments(first, second, target, strategy)));
        removeOption(args, "--seed");

        var generated = run(args.toArray(String[]::new));

        assertThat(generated.exitCode()).isZero();
        assertThat(generated.stderr()).isEmpty();
        var seedLine = generated.stdout().lines().findFirst().orElseThrow();
        assertThat(seedLine).matches("Seed: -?\\d+");
        long actualSeed = Long.parseLong(seedLine.substring("Seed: ".length()));

        var repeatArgs = new ArrayList<>(List.of(arguments(first, second, repeatedTarget, strategy)));
        repeatArgs.set(repeatArgs.indexOf("--seed") + 1, Long.toString(actualSeed));
        var repeated = run(repeatArgs.toArray(String[]::new));

        assertThat(repeated.exitCode()).isZero();
        assertThat(repeated.stderr()).isEmpty();
        assertThat(repeated.stdout()).startsWith(seedLine + "\n");
        assertThat(Files.readAllBytes(repeatedTarget)).isEqualTo(Files.readAllBytes(target));
    }

    @ParameterizedTest(name = "Seed {0}")
    @ValueSource(longs = {0, Long.MIN_VALUE, Long.MAX_VALUE})
    @DisplayName("Ноль и крайние значения общего seed не заменяются случайными")
    void preservesExplicitSeed(long seed, @TempDir Path directory) throws IOException {
        var first = directory.resolve("a.json");
        var second = directory.resolve("b.json");
        var target = directory.resolve("report.md");
        writeConfig(first, 2, false);
        writeConfig(second, 3, false);
        var args = new ArrayList<>(List.of(arguments(first, second, target, "random")));
        args.set(args.indexOf("--seed") + 1, Long.toString(seed));

        var result = run(args.toArray(String[]::new));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).startsWith("Seed: " + seed + "\n");
        assertThat(Files.readString(target, StandardCharsets.UTF_8)).contains("| Seed | " + seed + " |");
    }

    @ParameterizedTest(name = "Аргумент {0}, значение {1}")
    @CsvSource({
        "--config, <missing>",
        "--players, <missing>",
        "--output, <missing>",
        "--players, 0",
        "--players, -1",
        "--players, wrong",
        "--seed, wrong",
        "--strategy, unknown",
        "--output, report.txt"
    })
    @DisplayName("Ошибки аргументов сравнения отклоняются без создания отчёта")
    void rejectsInvalidArguments(String option, String value, @TempDir Path directory) throws IOException {
        var first = directory.resolve("a.json");
        var second = directory.resolve("b.json");
        var target = directory.resolve("report.md");
        writeConfig(first, 2, false);
        writeConfig(second, 3, false);
        var args = new ArrayList<>(List.of(arguments(first, second, target, "random")));
        if ("<missing>".equals(value)) {
            removeOption(args, option);
        } else {
            args.set(
                    args.indexOf(option) + 1,
                    "--output".equals(option) ? directory.resolve(value).toString() : value);
        }

        var result = run(args.toArray(String[]::new));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains(option);
        try (var files = Files.list(directory)) {
            assertThat(files.toList()).containsExactlyInAnyOrder(first, second);
        }
    }

    @Test
    @DisplayName("Для сравнения недостаточно одной конфигурации")
    void rejectsSingleConfig(@TempDir Path directory) throws IOException {
        var config = directory.resolve("a.json");
        var target = directory.resolve("report.md");
        writeConfig(config, 2, false);

        var result = run("compare", "--config", config.toString(), "--players", "8", "--output", target.toString());

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("--config", "двух");
        assertThat(Files.exists(target)).isFalse();
    }

    @ParameterizedTest(name = "Путь с точками: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("Повтор одного файла отклоняется и сохраняет предыдущий отчёт")
    void rejectsDuplicateConfig(boolean alias, @TempDir Path directory) throws IOException {
        var config = directory.resolve("a.json");
        var repeated = alias ? directory.resolve("nested/../a.json") : config;
        var target = directory.resolve("report.md");
        writeConfig(config, 2, false);
        Files.writeString(target, "previous report", StandardCharsets.UTF_8);

        var result = run(arguments(config, repeated, target, "random"));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("--config", "несколько раз");
        assertThat(Files.readString(target, StandardCharsets.UTF_8)).isEqualTo("previous report");
    }

    @ParameterizedTest(name = "Некорректный файл существует: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("Ошибка второй конфигурации сохраняет существующий отчёт")
    void preservesReportOnConfigError(boolean exists, @TempDir Path directory) throws IOException {
        var first = directory.resolve("a.json");
        var second = directory.resolve("b.json");
        var target = directory.resolve("report.md");
        writeConfig(first, 2, false);
        Files.writeString(target, "previous report", StandardCharsets.UTF_8);
        if (exists) {
            writeConfig(second, 3, false);
            var root = (ObjectNode) new ObjectMapper().readTree(second.toFile());
            root.put("movesLimit", 0);
            Files.writeString(second, root.toString(), StandardCharsets.UTF_8);
        }

        var result = run(arguments(first, second, target, "random"));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains(second.toString(), "конфигурац").doesNotContain("\tat ");
        assertThat(Files.readString(target, StandardCharsets.UTF_8)).isEqualTo("previous report");
    }

    @ParameterizedTest(name = "Индекс файла {0}")
    @ValueSource(ints = {0, 1})
    @DisplayName("Отчёт не может заменить ни одну из конфигураций")
    void rejectsOutputOverAnyInput(int index, @TempDir Path directory) throws IOException {
        var first = directory.resolve("a.md");
        var second = directory.resolve("b.md");
        writeConfig(first, 2, false);
        writeConfig(second, 3, false);
        var originalFirst = Files.readAllBytes(first);
        var originalSecond = Files.readAllBytes(second);
        var source = index == 0 ? first : second;
        var target = directory.resolve("nested").resolve("..").resolve(source.getFileName());

        var result = run(arguments(first, second, target, "random"));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("--output", "конфигурации");
        assertThat(Files.readAllBytes(first)).isEqualTo(originalFirst);
        assertThat(Files.readAllBytes(second)).isEqualTo(originalSecond);
    }

    @ParameterizedTest(name = "Цель {0}")
    @ValueSource(strings = {"missing/report.md", "directory.md"})
    @DisplayName("Ошибка записи общего отчёта даёт код 1 и не оставляет временных файлов")
    void handlesWriteFailure(String name, @TempDir Path directory) throws IOException {
        var first = directory.resolve("a.json");
        var second = directory.resolve("b.json");
        var target = directory.resolve(name);
        writeConfig(first, 2, false);
        writeConfig(second, 3, false);
        if ("directory.md".equals(name)) {
            Files.createDirectory(target);
        }

        var result = run(arguments(first, second, target, "random"));

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("Не удалось сохранить отчёт", target.toString());
        var expectedPaths = new ArrayList<>(List.of(first, second));
        if (Files.isDirectory(target)) {
            expectedPaths.add(target);
        }
        try (var files = Files.list(directory)) {
            assertThat(files.toList()).containsExactlyInAnyOrderElementsOf(expectedPaths);
        }
    }

    @Test
    @DisplayName("Приложение отдельным процессом выполняет compare и сохраняет общий отчёт")
    void runsAsSeparateProcess(@TempDir Path directory) throws IOException {
        var first = directory.resolve("a.json");
        var second = directory.resolve("b.json");
        var target = directory.resolve("report.html");
        writeConfig(first, 2, false);
        writeConfig(second, 3, false);

        var result = CliRunner.run(arguments(first, second, target, "greedy-score"));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).startsWith("Seed: -42").contains("Отчёт сохранён:");
        assertThat(Files.readString(target, StandardCharsets.UTF_8)).startsWith("<!DOCTYPE html>");
    }

    private static String[] arguments(Path first, Path second, Path target, String strategy) {
        return new String[] {
            "compare",
            "--config",
            first.toString(),
            "--config",
            second.toString(),
            "--players",
            "8",
            "--seed",
            "-42",
            "--strategy",
            strategy,
            "--output",
            target.toString()
        };
    }

    private static void removeOption(List<String> args, String option) {
        int index = args.indexOf(option);
        while (index >= 0) {
            args.remove(index + 1);
            args.remove(index);
            index = args.indexOf(option);
        }
    }

    private static void writeConfig(Path path, int size, boolean spawnMaxTile) throws IOException {
        Files.createDirectories(path.getParent());
        var root = (ObjectNode) new ObjectMapper().readTree(EXAMPLE_CONFIG.toFile());
        root.put("boardSize", size);
        root.put("movesLimit", 8);
        if (spawnMaxTile) {
            var rules = root.putArray("spawnRules");
            rules.addObject().put("cashback", "32").put("probability", "1");
        }
        Files.writeString(path, root.toString(), StandardCharsets.UTF_8);
    }

    private static Result run(String... args) {
        var stdout = new StringWriter();
        var stderr = new StringWriter();
        int exitCode;
        try (var reader = new StringReader("");
                var out = new PrintWriter(stdout, true);
                var err = new PrintWriter(stderr, true)) {
            exitCode = Main.createCommandLine(reader).setOut(out).setErr(err).execute(args);
        }
        return new Result(
                exitCode,
                stdout.toString().replace("\r\n", "\n"),
                stderr.toString().replace("\r\n", "\n"));
    }

    private record Result(int exitCode, String stdout, String stderr) {}
}
