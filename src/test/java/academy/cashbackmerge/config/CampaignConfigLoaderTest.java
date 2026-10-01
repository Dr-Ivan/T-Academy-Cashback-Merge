package academy.cashbackmerge.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import academy.cashbackmerge.support.CampaignFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Загрузка JSON-конфигурации кампании")
class CampaignConfigLoaderTest {

    private static final ObjectMapper TEST_MAPPER = new ObjectMapper();

    private static final String VALID_JSON = """
            {
              "boardSize": 4,
              "movesLimit": 100,
              "spawnRules": [
                {"cashback": "1", "probability": "0.85"},
                {"cashback": "2", "probability": "0.15"}
              ],
              "rewardTable": [
                {"cashback": "2", "reward": "0"},
                {"cashback": "32", "reward": "2000"}
              ],
              "maxRewardCashback": "32",
              "maxRewardAllowedRate": "0.20"
            }
            """;

    @TempDir
    private Path directory;

    @Test
    @DisplayName("Пример кампании из проекта загружается вместе со служебным полем _note")
    void loadsExampleCampaign() {
        var path = Path.of("config", "campaign.example.json");

        var config = new CampaignConfigLoader().load(path);

        assertThat(config).isEqualTo(CampaignFixtures.validConfig());
    }

    @ParameterizedTest(name = "Размер поля {0} × {0}")
    @ValueSource(ints = {2, 5})
    @DisplayName("Размер поля считывается из файла конфигурации")
    void loadsConfiguredBoardSize(int size) throws IOException {
        var json = changedJson(root -> root.put("boardSize", size));
        var path = writeConfig(json);

        var config = new CampaignConfigLoader().load(path);

        assertThat(config.boardSize()).isEqualTo(size);
    }

    @Test
    @DisplayName("Вероятности и награды загружаются без потери десятичной точности")
    void preservesDecimalPrecision() throws IOException {
        var json = changedJson(root -> {
            spawnRule(root, 0).put("probability", "0.85000000000000000001");
            spawnRule(root, 1).put("probability", "0.14999999999999999999");

            var highestReward = (ObjectNode) root.path("rewardTable").path(1);
            highestReward.put("reward", "2000.01");
        });
        var path = writeConfig(json);

        var config = new CampaignConfigLoader().load(path);

        assertThat(config.spawnRules().getFirst().probability())
                .isEqualByComparingTo(new BigDecimal("0.85000000000000000001"));
        assertThat(config.spawnRules().getLast().probability())
                .isEqualByComparingTo(new BigDecimal("0.14999999999999999999"));
        assertThat(config.rewardTable().getLast().reward()).isEqualByComparingTo(new BigDecimal("2000.01"));
    }

    @ParameterizedTest(name = "Ошибка формата, пример {index}: {1}")
    @MethodSource("invalidDocuments")
    @DisplayName("Ошибки структуры и типов JSON отклоняются с указанием файла и поля")
    void rejectsInvalidDocument(String json, String message) throws IOException {
        var path = writeConfig(json);

        assertThatThrownBy(() -> new CampaignConfigLoader().load(path))
                .isInstanceOf(ConfigLoadException.class)
                .hasMessageContaining(path.toString())
                .hasMessageContaining(message);
    }

    @Test
    @DisplayName("Загрузчик применяет бизнес-валидацию и сохраняет причину ошибки")
    void wrapsDomainValidationFailure() throws IOException {
        var json = changedJson(root -> root.put("movesLimit", 0));
        var path = writeConfig(json);

        assertThatThrownBy(() -> new CampaignConfigLoader().load(path))
                .isInstanceOf(ConfigLoadException.class)
                .hasMessageContaining(path.toString())
                .hasMessageContaining("movesLimit")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Отсутствующий файл вызывает ошибку чтения с сохранённой причиной")
    void wrapsMissingFileFailure() {
        var path = directory.resolve("missing.json");

        assertThatThrownBy(() -> new CampaignConfigLoader().load(path))
                .isInstanceOf(ConfigLoadException.class)
                .hasMessageContaining(path.toString())
                .hasCauseInstanceOf(IOException.class);
    }

    private static Stream<Arguments> invalidDocuments() throws IOException {
        return Stream.of(
                Arguments.of("", "пуст"),
                Arguments.of("null", "конфигурация"),
                Arguments.of("[]", "конфигурация"),
                Arguments.of("{", "JSON"),
                Arguments.of(VALID_JSON + "{}", "JSON"),
                Arguments.of(changedJson(root -> root.remove("movesLimit")), "movesLimit"),
                Arguments.of(changedJson(root -> root.putNull("movesLimit")), "movesLimit"),
                Arguments.of(changedJson(root -> root.put("unexpected", true)), "unexpected"),
                Arguments.of(VALID_JSON.replace("\"boardSize\": 4", "\"boardSize\": 4, \"boardSize\": 5"), "boardSize"),
                Arguments.of(changedJson(root -> root.put("boardSize", "4")), "boardSize"),
                Arguments.of(changedJson(root -> root.put("boardSize", new BigDecimal("4.5"))), "boardSize"),
                Arguments.of(changedJson(root -> root.put("boardSize", 2_147_483_648L)), "boardSize"),
                Arguments.of(changedJson(root -> root.put("maxRewardAllowedRate", true)), "maxRewardAllowedRate"),
                Arguments.of(changedJson(root -> root.set("spawnRules", root.objectNode())), "spawnRules"),
                Arguments.of(changedJson(root -> root.putArray("spawnRules").addNull()), "spawnRules[0]"),
                Arguments.of(
                        changedJson(root -> spawnRule(root, 0).put("probability", new BigDecimal("0.85"))),
                        "spawnRules[0].probability"),
                Arguments.of(
                        changedJson(root -> spawnRule(root, 0).put("probability", "not-a-number")),
                        "spawnRules[0].probability"),
                Arguments.of(changedJson(root -> spawnRule(root, 0).remove("cashback")), "spawnRules[0].cashback"),
                Arguments.of(changedJson(root -> spawnRule(root, 0).put("cashback", "64")), "spawnRules[0].cashback"),
                Arguments.of(
                        changedJson(root -> spawnRule(root, 0).put("probabilty", "0.85")), "spawnRules[0].probabilty"));
    }

    private Path writeConfig(String json) throws IOException {
        var path = directory.resolve("campaign.json");
        return Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private static String changedJson(Consumer<ObjectNode> change) throws IOException {
        var root = (ObjectNode) TEST_MAPPER.readTree(VALID_JSON);
        change.accept(root);
        return root.toString();
    }

    private static ObjectNode spawnRule(ObjectNode root, int index) {
        return (ObjectNode) root.path("spawnRules").path(index);
    }
}
