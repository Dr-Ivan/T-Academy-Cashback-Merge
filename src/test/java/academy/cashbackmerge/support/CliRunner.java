package academy.cashbackmerge.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Запускает программу отдельным процессом и возвращает её вывод и код возврата.
 *
 * <p>Отдельный процесс нужен потому, что {@code System.exit(...)} завершает всю JVM: вызови {@code Main.main(...)}
 * прямо из теста — и упадёт весь прогон тестов.
 *
 * <p>Класс дан готовым, писать его не нужно. Пример:
 *
 * <pre>{@code
 * var result = CliRunner.run("simulate", "--config", "campaign.json", "--players", "1000");
 *
 * assertThat(result.exitCode()).isZero();
 * assertThat(result.stdout()).contains("OK");
 * }</pre>
 */
public final class CliRunner {

    private static final int TIMEOUT_SECONDS = 30;

    private CliRunner() {}

    /**
     * Результат запуска программы.
     *
     * @param exitCode код возврата
     * @param stdout стандартный вывод
     * @param stderr поток ошибок, туда же идут логи
     */
    public record Result(int exitCode, String stdout, String stderr) {
        @Override
        public String toString() {
            return "exitCode=%d%nstdout:%n%s%nstderr:%n%s".formatted(exitCode, stdout, stderr);
        }
    }

    /** Запуск с аргументами, без ввода с клавиатуры. */
    public static Result run(String... args) {
        return run(null, null, args);
    }

    /** Запуск с подачей строк на стандартный ввод. */
    public static Result runWithInput(String stdin, String... args) {
        return run(null, stdin, args);
    }

    /**
     * Полная форма запуска.
     *
     * @param workingDirectory рабочая директория; {@code null} — текущая. Пригодится с {@code @TempDir}, когда
     *     программа пишет файлы
     * @param stdin данные для стандартного ввода или {@code null}
     * @param args аргументы командной строки
     */
    public static Result run(Path workingDirectory, String stdin, String... args) {
        var command = buildCommand(args);
        var processBuilder = new ProcessBuilder(command);
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory.toFile());
        }

        try {
            var process = processBuilder.start();
            writeStdin(process, stdin);

            var stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException(
                        "Программа не завершилась за %d с — похоже, ждёт ввода, которого нет.%nstdout:%n%s"
                                .formatted(TIMEOUT_SECONDS, stdout));
            }

            return new Result(process.exitValue(), stdout, stderr);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось запустить программу: " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Запуск программы прерван", e);
        }
    }

    private static List<String> buildCommand(String... args) {
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-Dfile.encoding=UTF-8");
        command.add("-Dstdout.encoding=UTF-8");
        command.add("-Dstderr.encoding=UTF-8");
        command.add("-cp");
        command.add(requiredProperty("academy.cli.classpath"));
        command.add(requiredProperty("academy.cli.mainClass"));
        command.addAll(List.of(args));
        return command;
    }

    private static void writeStdin(Process process, String stdin) throws IOException {
        try (var input = process.getOutputStream()) {
            if (stdin != null) {
                input.write(stdin.getBytes(StandardCharsets.UTF_8));
                input.flush();
            }
        }
    }

    private static String requiredProperty(String name) {
        var value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("""
                    Не задано системное свойство '%s'.

                    Тест запущен в обход Gradle. Запусти './gradlew test' или включи в IDEA:
                    Settings -> Build Tools -> Gradle -> Run tests using -> Gradle.""".formatted(name));
        }
        return value;
    }
}
