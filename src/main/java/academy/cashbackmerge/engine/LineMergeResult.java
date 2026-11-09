package academy.cashbackmerge.engine;

import academy.cashbackmerge.model.Cashback;
import java.util.List;
import java.util.Optional;

/** Неизменяемые ячейки линии после слияния и прибавка к счёту. */
public record LineMergeResult(List<Optional<Cashback>> cells, long scoreDelta) {

    public LineMergeResult {
        cells = List.copyOf(cells);
        if (scoreDelta < 0) {
            throw new IllegalArgumentException("Прибавка к счёту не должна быть отрицательной: " + scoreDelta);
        }
    }
}
