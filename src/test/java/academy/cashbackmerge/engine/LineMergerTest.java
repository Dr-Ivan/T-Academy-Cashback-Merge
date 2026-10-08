package academy.cashbackmerge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import academy.cashbackmerge.model.Cashback;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Слияние одной линии")
class LineMergerTest {

    private final LineMerger merger = new LineMerger();

    @ParameterizedTest(name = "{0} -> {1}, score +{2}")
    @CsvSource({
        "'.,1,.,1', '2,.,.,.', 2",
        "'.,2,4,.', '2,4,.,.', 0",
        "'2,4,8,16', '2,4,8,16', 0",
        "'16,16,8,8', '32,16,.,.', 48",
        "'32,32,16,16', '32,32,32,.', 32",
        "'.,.,.,.', '.,.,.,.', 0",
        "'1,1,1,1,1', '2,2,1,.,.', 4",
    })
    @DisplayName("Слияние учитывает пустоты, максимальную плитку, размер линии и сумму очков")
    void mergesLineAndCalculatesScore(String before, String expected, long scoreDelta) {
        var result = merger.merge(parseRow(before));

        assertThat(result.cells()).isEqualTo(parseRow(expected));
        assertThat(result.scoreDelta()).isEqualTo(scoreDelta);
    }

    @Test
    @DisplayName("Слияние сохраняет исходные ячейки и возвращает независимый неизменяемый результат")
    void preservesSourceAndReturnsImmutableResult() {
        var original = parseRow(".,1,.,1");
        var source = new ArrayList<>(original);

        var result = merger.merge(source);
        assertThat(source).isEqualTo(original);

        source.set(0, Optional.of(Cashback.of(32)));

        assertThat(result.cells()).isEqualTo(parseRow("2,.,.,."));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> result.cells().set(0, Optional.empty()));
    }

    private static List<Optional<Cashback>> parseRow(String text) {
        return Arrays.stream(text.split(","))
                .map(cell -> ".".equals(cell)
                        ? Optional.<Cashback>empty()
                        : Optional.of(Cashback.of(Integer.parseInt(cell))))
                .toList();
    }
}
