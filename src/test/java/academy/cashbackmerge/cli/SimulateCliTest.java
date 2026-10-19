package academy.cashbackmerge.cli;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.Main;
import academy.cashbackmerge.config.CampaignConfigLoader;
import academy.cashbackmerge.presentation.ReportFormat;
import academy.cashbackmerge.presentation.SimulationReportFormatter;
import academy.cashbackmerge.simulation.SimulationRunner;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Симуляция кампании через CLI")
class SimulateCliTest {

    private static final Path EXAMPLE_CONFIG = Path.of("config", "campaign.example.json");

    @Test
    @DisplayName("Справка simulate доступна без обязательных аргументов")
    void showsHelp() {
        var result = run("simulate", "--help");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).contains("--config", "--players", "--seed", "--strategy", "--output", "random");
    }

    @ParameterizedTest(name = "Стратегия {0}, формат {1}, поле {2} × {2}")
    @CsvSource({"random, md, 2", "' GrEeDy-ScOrE ', HTML, 5"})
    @DisplayName("CLI передаёт параметры симуляции и заменяет отчёт в UTF-8 по пути с пробелами")
    void savesConfiguredReport(String strategyName, String extension, int size, @TempDir Path directory)
            throws IOException {
        var configPath = directory.resolve("кампания с пробелами.json");
        var target = directory.resolve("отчёт с пробелами." + extension);
        writeConfig(configPath, size, false);
        Files.writeString(target, "previous report", StandardCharsets.UTF_8);

        var result = run(arguments(configPath, target, strategyName));
        var config = new CampaignConfigLoader().load(configPath);
        var report = new SimulationRunner(config).simulate(8, PlayerStrategy.fromCliName(strategyName), -42);
        var expected = new SimulationReportFormatter().format(report, ReportFormat.fromPath(target));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).contains("Отчёт сохранён:", target.toString(), "Статус кампании:");
        assertThat(Files.readAllBytes(target)).isEqualTo(expected.getBytes(StandardCharsets.UTF_8));
        try (var files = Files.list(directory)) {
            assertThat(files.toList()).containsExactlyInAnyOrder(configPath, target);
        }
    }

    @Test
    @DisplayName("При отсутствии strategy используется random")
    void defaultsToRandomStrategy(@TempDir Path directory) throws IOException {
        var config = directory.resolve("campaign.json");
        var first = directory.resolve("default.md");
        var second = directory.resolve("explicit.md");
        writeConfig(config, 2, false);
        var args = new ArrayList<>(List.of(arguments(config, first, "random")));
        int index = args.indexOf("--strategy");
        args.remove(index + 1);
        args.remove(index);

        var implicit = run(args.toArray(String[]::new));
        var explicit = run(arguments(config, second, "random"));

        assertThat(implicit.exitCode()).isZero();
        assertThat(explicit.exitCode()).isZero();
        assertThat(implicit.stderr()).isEmpty();
        assertThat(explicit.stderr()).isEmpty();
        assertThat(Files.readAllBytes(first)).isEqualTo(Files.readAllBytes(second));
    }

    @Test
    @DisplayName("Бизнес-статус FAILED сохраняется в отчёт и не означает ошибку выполнения")
    void savesFailedBusinessStatusSuccessfully(@TempDir Path directory) throws IOException {
        var config = directory.resolve("campaign.json");
        var target = directory.resolve("report.md");
        writeConfig(config, 2, true);

        var result = run(arguments(config, target, "random"));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).contains("Статус кампании: FAILED");
        assertThat(Files.readString(target, StandardCharsets.UTF_8)).contains("| Статус | FAILED |");
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
    @DisplayName("Ошибки аргументов отклоняются до создания отчёта")
    void rejectsInvalidArguments(String option, String value, @TempDir Path directory) throws IOException {
        var config = directory.resolve("campaign.json");
        var target = directory.resolve("report.md");
        writeConfig(config, 2, false);
        var args = new ArrayList<>(List.of(arguments(config, target, "random")));
        int index = args.indexOf(option);
        if ("<missing>".equals(value)) {
            args.remove(index + 1);
            args.remove(index);
        } else {
            args.set(
                    index + 1,
                    "--output".equals(option) ? directory.resolve(value).toString() : value);
        }

        var result = run(args.toArray(String[]::new));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains(option);
        try (var files = Files.list(directory)) {
            assertThat(files.toList()).containsExactly(config);
        }
    }

    @ParameterizedTest(name = "Некорректный файл существует: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("Ошибка конфигурации сохраняет предыдущий отчёт")
    void preservesReportOnConfigError(boolean exists, @TempDir Path directory) throws IOException {
        var config = directory.resolve("campaign.json");
        var target = directory.resolve("report.md");
        Files.writeString(target, "previous report", StandardCharsets.UTF_8);
        if (exists) {
            writeConfig(config, 2, false);
            var root = (ObjectNode) new ObjectMapper().readTree(config.toFile());
            root.put("movesLimit", 0);
            Files.writeString(config, root.toString(), StandardCharsets.UTF_8);
        }

        var result = run(arguments(config, target, "random"));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("конфигурац").doesNotContain("ConfigLoadException", "\tat ");
        assertThat(Files.readString(target, StandardCharsets.UTF_8)).isEqualTo("previous report");
    }

    @Test
    @DisplayName("Файл конфигурации нельзя заменить отчётом даже через путь с точками")
    void rejectsOutputOverInput(@TempDir Path directory) throws IOException {
        var config = directory.resolve("campaign.md");
        writeConfig(config, 2, false);
        var original = Files.readAllBytes(config);
        var target = directory.resolve("nested").resolve("..").resolve("campaign.md");

        var result = run(arguments(config, target, "random"));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stderr()).contains("--output", "конфигурации");
        assertThat(Files.readAllBytes(config)).isEqualTo(original);
    }

    @ParameterizedTest(name = "Цель {0}")
    @ValueSource(strings = {"missing/report.md", "directory.md"})
    @DisplayName("Ошибка записи даёт код 1 и не оставляет временный файл")
    void handlesWriteFailure(String name, @TempDir Path directory) throws IOException {
        var config = directory.resolve("campaign.json");
        var target = directory.resolve(name);
        writeConfig(config, 2, false);
        if ("directory.md".equals(name)) {
            Files.createDirectory(target);
            Files.writeString(target.resolve("keep.txt"), "keep", StandardCharsets.UTF_8);
        }

        var result = run(arguments(config, target, "random"));

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("Не удалось сохранить отчёт", target.toString());
        if (Files.isDirectory(target)) {
            assertThat(Files.readString(target.resolve("keep.txt"), StandardCharsets.UTF_8))
                    .isEqualTo("keep");
        } else {
            assertThat(target).doesNotExist();
        }
        try (var files = Files.list(directory)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .noneMatch(fileName -> fileName.startsWith("cashback-merge-"));
        }
    }

    @Test
    @DisplayName("Приложение отдельным процессом сохраняет HTML-отчёт")
    void savesHtmlInApplicationProcess(@TempDir Path directory) throws IOException {
        var config = directory.resolve("campaign.json");
        var target = directory.resolve("report.html");
        writeConfig(config, 2, false);

        var result = CliRunner.run(arguments(config, target, "greedy-score"));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).contains("Отчёт сохранён:");
        assertThat(Files.readString(target, StandardCharsets.UTF_8)).startsWith("<!DOCTYPE html>");
    }

    @ParameterizedTest(name = "Стратегия {0}, формат {1}")
    @CsvSource({"random, md", "greedy-score, html"})
    @DisplayName("Случайный seed печатается и позволяет точно воспроизвести файл отчёта")
    void reproducesReportWithGeneratedSeed(String strategy, String extension, @TempDir Path directory)
            throws IOException {
        var config = directory.resolve("campaign.json");
        var first = directory.resolve("generated." + extension);
        var second = directory.resolve("repeated." + extension);
        writeConfig(config, 2, false);
        var args = new ArrayList<>(List.of(arguments(config, first, strategy)));
        int index = args.indexOf("--seed");
        args.remove(index + 1);
        args.remove(index);

        var generated = run(args.toArray(String[]::new));

        assertThat(generated.exitCode()).isZero();
        assertThat(generated.stderr()).isEmpty();
        var seedLine = generated.stdout().lines().findFirst().orElseThrow();
        assertThat(seedLine).matches("Seed: -?\\d+");
        long actualSeed = Long.parseLong(seedLine.substring("Seed: ".length()));

        var repeatArgs = new ArrayList<>(List.of(arguments(config, second, strategy)));
        repeatArgs.set(repeatArgs.indexOf("--seed") + 1, Long.toString(actualSeed));
        var repeated = run(repeatArgs.toArray(String[]::new));

        assertThat(repeated.exitCode()).isZero();
        assertThat(repeated.stderr()).isEmpty();
        assertThat(repeated.stdout()).startsWith(seedLine + "\n");
        assertThat(Files.readAllBytes(second)).isEqualTo(Files.readAllBytes(first));
    }

    @ParameterizedTest(name = "Seed {0}")
    @ValueSource(longs = {0, Long.MIN_VALUE, Long.MAX_VALUE})
    @DisplayName("Явно заданные ноль и крайние значения seed используются без замены")
    void preservesExplicitSeed(long seed, @TempDir Path directory) throws IOException {
        var config = directory.resolve("campaign.json");
        var target = directory.resolve("report.md");
        writeConfig(config, 2, false);
        var args = new ArrayList<>(List.of(arguments(config, target, "random")));
        args.set(args.indexOf("--seed") + 1, Long.toString(seed));

        var result = run(args.toArray(String[]::new));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).startsWith("Seed: " + seed + "\n");
        assertThat(Files.readString(target, StandardCharsets.UTF_8)).contains("| Seed | " + seed + " |");
    }

    private static String[] arguments(Path config, Path target, String strategy) {
        return new String[] {
            "simulate",
            "--config",
            config.toString(),
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

    private static void writeConfig(Path path, int size, boolean spawnMaxTile) throws IOException {
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
