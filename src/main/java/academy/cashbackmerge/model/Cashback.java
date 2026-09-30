package academy.cashbackmerge.model;

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
}
