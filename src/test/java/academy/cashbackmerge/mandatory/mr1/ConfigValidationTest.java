package academy.cashbackmerge.mandatory.mr1;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: конфиг кампании. */
@DisplayName("MR1. Конфиг кампании")
class ConfigValidationTest {

    @Test
    @Disabled("MR1: реализуй тест и удали эту строку")
    @DisplayName("Валидный конфиг принимается")
    void validConfigIsAccepted() {
        fail("Тест не реализован");
    }

    @Test
    @Disabled("MR1: реализуй тест и удали эту строку")
    @DisplayName("Сумма вероятностей spawn-правил не равна 1 — конфиг отклоняется")
    void probabilitiesNotSummingToOneAreRejected() {
        fail("Тест не реализован");
    }

    @Test
    @Disabled("MR1: реализуй тест и удали эту строку")
    @DisplayName("movesLimit <= 0 — конфиг отклоняется")
    void nonPositiveMovesLimitIsRejected() {
        fail("Тест не реализован");
    }

    @Test
    @Disabled("MR1: реализуй тест и удали эту строку")
    @DisplayName("Пустой список spawn-правил — конфиг отклоняется")
    void emptySpawnRulesAreRejected() {
        fail("Тест не реализован");
    }
}
