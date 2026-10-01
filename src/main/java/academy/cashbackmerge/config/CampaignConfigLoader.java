package academy.cashbackmerge.config;

import academy.cashbackmerge.model.Cashback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/** Загрузка и проверка JSON-конфигурации кампании. */
public final class CampaignConfigLoader {

    private static final Set<String> ROOT_FIELDS = Set.of(
            "_note",
            "boardSize",
            "movesLimit",
            "spawnRules",
            "rewardTable",
            "maxRewardCashback",
            "maxRewardAllowedRate");

    private static final Set<String> SPAWN_FIELDS = Set.of("cashback", "probability");
    private static final Set<String> REWARD_FIELDS = Set.of("cashback", "reward");

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    public CampaignConfig load(Path path) {
        Objects.requireNonNull(path, "Путь к конфигурации не должен быть null");

        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var root = MAPPER.readTree(reader);

            if (root == null || root.isMissingNode()) {
                throw new IllegalArgumentException("Файл конфигурации пуст");
            }

            return parseConfig(root);

        } catch (JsonProcessingException exception) {
            throw new ConfigLoadException(
                    "Некорректный JSON в " + path + ": " + exception.getOriginalMessage(), exception);
        } catch (IOException exception) {
            throw new ConfigLoadException("Не удалось прочитать конфигурацию: " + path, exception);
        } catch (IllegalArgumentException exception) {
            throw new ConfigLoadException(
                    "Некорректная конфигурация " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static CampaignConfig parseConfig(JsonNode root) {
        requireObject(root, "конфигурация", ROOT_FIELDS);

        return new CampaignConfig(
                readInteger(root, "boardSize"),
                readInteger(root, "movesLimit"),
                readArray(root, "spawnRules", CampaignConfigLoader::readSpawnRule),
                readArray(root, "rewardTable", CampaignConfigLoader::readRewardTier),
                readCashback(root, "maxRewardCashback", ""),
                readDecimal(root, "maxRewardAllowedRate", ""));
    }

    private static SpawnRule readSpawnRule(JsonNode node, String path) {
        requireObject(node, path, SPAWN_FIELDS);

        return new SpawnRule(readCashback(node, "cashback", path), readDecimal(node, "probability", path));
    }

    private static RewardTier readRewardTier(JsonNode node, String path) {
        requireObject(node, path, REWARD_FIELDS);

        return new RewardTier(readCashback(node, "cashback", path), readDecimal(node, "reward", path));
    }

    private static <T> List<T> readArray(JsonNode root, String field, BiFunction<JsonNode, String, T> converter) {
        var array = requiredField(root, field, "");

        if (!array.isArray()) {
            throw new IllegalArgumentException(field + ": ожидается массив");
        }
        var result = new ArrayList<T>();
        int index = 0;
        for (var item : array) {
            result.add(converter.apply(item, field + "[" + index + "]"));
            index++;
        }
        return List.copyOf(result);
    }

    private static int readInteger(JsonNode node, String field) {
        var value = requiredField(node, field, "");

        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IllegalArgumentException(field + ": ожидается целое JSON-число в диапазоне int");
        }
        return value.intValue();
    }

    private static Cashback readCashback(JsonNode node, String field, String path) {
        var value = readString(node, field, path);

        try {
            return Cashback.of(Integer.parseInt(value));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    fieldPath(path, field) + ": ожидается строка с допустимым целым уровнем кэшбэка: " + value,
                    exception);
        }
    }

    private static BigDecimal readDecimal(JsonNode node, String field, String path) {
        var value = readString(node, field, path);

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    fieldPath(path, field) + ": ожидается строка с десятичным числом: " + value, exception);
        }
    }

    private static String readString(JsonNode node, String field, String path) {
        var value = requiredField(node, field, path);

        if (!value.isTextual()) {
            throw new IllegalArgumentException(fieldPath(path, field) + ": ожидается строка");
        }
        return value.asText();
    }

    private static JsonNode requiredField(JsonNode node, String field, String path) {
        var value = node.path(field);

        if (value.isMissingNode() || value.isNull()) {
            throw new IllegalArgumentException(
                    fieldPath(path, field) + ": обязательное поле отсутствует или равно null");
        }
        return value;
    }

    private static void requireObject(JsonNode node, String path, Set<String> allowedFields) {
        if (!node.isObject()) {
            throw new IllegalArgumentException(path + ": ожидается JSON-объект");
        }
        for (var property : node.properties()) {
            if (!allowedFields.contains(property.getKey())) {
                throw new IllegalArgumentException(fieldPath(path, property.getKey()) + ": неизвестное поле");
            }
        }
    }

    private static String fieldPath(String path, String field) {
        return path.isEmpty() ? field : path + "." + field;
    }
}
