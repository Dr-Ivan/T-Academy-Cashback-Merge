package academy.cashbackmerge.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Неизменяемое квадратное поле, хранящее только занятые клетки. */
public record Board(int size, Map<Position, Cashback> tiles) {

    private static final int MIN_SIZE = 2;

    public Board {
        if (size < MIN_SIZE) {
            throw new IllegalArgumentException("Размер поля должен быть не меньше " + MIN_SIZE + ": " + size);
        }

        tiles = Map.copyOf(tiles);

        for (var position : tiles.keySet()) {
            validatePosition(size, position);
        }
    }

    public static Board empty(int size) {
        return new Board(size, Map.of());
    }

    public Optional<Cashback> tileAt(Position position) {
        validatePosition(size, position);
        return Optional.ofNullable(tiles.get(position));
    }

    public Board withTile(Position position, Cashback cashback) {
        validatePosition(size, position);
        Objects.requireNonNull(cashback, "Плитка не должна быть null");

        if (tiles.containsKey(position)) {
            throw new IllegalArgumentException("Клетка уже занята: " + position);
        }

        var updatedTiles = new HashMap<>(tiles);
        updatedTiles.put(position, cashback);
        return new Board(size, updatedTiles);
    }

    private static void validatePosition(int size, Position position) {
        Objects.requireNonNull(position, "Координаты клетки не должны быть null");

        if (position.row() >= size || position.column() >= size) {
            throw new IllegalArgumentException("Клетка находится за границами поля размером " + size + ": " + position);
        }
    }
}
