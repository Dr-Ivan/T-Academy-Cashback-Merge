package academy.cashbackmerge.engine;

import academy.cashbackmerge.model.Cashback;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Сдвиг и слияние линии к её началу без появления новых плиток. */
public final class LineMerger {

    public LineMergeResult merge(List<Optional<Cashback>> line) {
        Objects.requireNonNull(line, "Линия не должна быть null");

        var packed = new ArrayList<Cashback>();
        for (var cell : line) {
            Objects.requireNonNull(cell, "Ячейка линии не должна быть null").ifPresent(packed::add);
        }

        var merged = new ArrayList<Optional<Cashback>>(line.size());
        long scoreDelta = 0;
        int index = 0;

        while (index < packed.size()) {
            var tile = packed.get(index);
            if (index + 1 < packed.size() && tile.canMergeWith(packed.get(index + 1))) {
                tile = tile.mergeWith(packed.get(index + 1));
                scoreDelta += tile.value();
                index += 2;
            } else {
                index++;
            }
            merged.add(Optional.of(tile));
        }
        while (merged.size() < line.size()) {
            merged.add(Optional.empty());
        }
        return new LineMergeResult(merged, scoreDelta);
    }
}
