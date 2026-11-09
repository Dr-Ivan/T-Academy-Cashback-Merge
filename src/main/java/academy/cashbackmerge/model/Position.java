package academy.cashbackmerge.model;

public record Position(int row, int column) {

    public Position {
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException(
                    "Координаты клетки не должны быть отрицательными: " + row + ", " + column);
        }
    }
}
