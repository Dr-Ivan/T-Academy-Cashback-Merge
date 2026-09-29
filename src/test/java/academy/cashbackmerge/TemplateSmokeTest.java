package academy.cashbackmerge;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.support.CliRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Проверка окружения. Если эти тесты упали — дело в настройке, а не в задании, см. README. */
@DisplayName("Проверка окружения")
class TemplateSmokeTest {

    private static final Path EXAMPLE_CONFIG = Path.of("config", "campaign.example.json");

    @Test
    @DisplayName("Приложение запускается и завершается с кодом 0")
    void applicationStartsAndExitsCleanly() {
        var result = CliRunner.run();

        assertThat(result.exitCode())
                .as("Программа завершилась с ненулевым кодом.%n%s", result)
                .isZero();
        assertThat(result.stdout()).as("Программа ничего не напечатала").isNotBlank();
    }

    @Test
    @DisplayName("Пример конфига кампании — валидный JSON и читается Jackson'ом")
    void exampleCampaignConfigIsValidJson() {
        assertThat(EXAMPLE_CONFIG).exists();

        var tree = readTree();

        assertThat(tree.has("spawnRules"))
                .as("В конфиге должен быть блок spawnRules")
                .isTrue();
        assertThat(tree.get("spawnRules"))
                .as("spawnRules не должен быть пустым")
                .isNotEmpty();
        assertThat(tree.get("spawnRules").get(0).get("probability").isTextual())
                .as("Вероятности должны храниться строками, а не числами с плавающей точкой")
                .isTrue();
    }

    private static JsonNode readTree() {
        try {
            return new ObjectMapper().readTree(Files.readString(EXAMPLE_CONFIG));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать " + EXAMPLE_CONFIG, e);
        }
    }
}
