package academy.cashbackmerge.model;

import java.util.Objects;

/** Значение плитки. */
public record Cashback(int value) {

    private static final int MAX_VALUE = 32;

    public Cashback {
        if (value <= 0 || value > MAX_VALUE || Integer.bitCount(value) != 1) {
            throw new IllegalArgumentException("Значение кэшбэка должно быть степенью двойки от 1 до 32: " + value);
        }
    }

    public static Cashback of(int value) {
        return new Cashback(value);
    }

    public boolean canMergeWith(Cashback other) {
        Objects.requireNonNull(other, "Вторая плитка не должна быть null");
        return value == other.value() && value < MAX_VALUE;
    }

    public Cashback mergeWith(Cashback other) {
        if (!canMergeWith(other)) {
            throw new IllegalArgumentException("Плитки нельзя объединить: " + value + " и " + other.value());
        }
        return Cashback.of(value * 2);
    }
}
