package academy.cashbackmerge.engine;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.Position;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.random.RandomGenerator;

/** Появление одной плитки по точным весам из конфигурации кампании. */
public final class TileSpawner {

    private final List<WeightedTile> tiles;
    private final BigInteger totalWeight;

    public TileSpawner(CampaignConfig config) {
        Objects.requireNonNull(config, "Конфигурация не должна быть null");

        int scale = config.spawnRules().stream()
                .mapToInt(rule -> rule.probability().scale())
                .max()
                .orElseThrow();

        var weightedTiles = new ArrayList<WeightedTile>();
        var cumulativeWeight = BigInteger.ZERO;

        for (var rule : config.spawnRules()) {
            var weight = rule.probability().movePointRight(scale).toBigIntegerExact();
            cumulativeWeight = cumulativeWeight.add(weight);
            weightedTiles.add(new WeightedTile(rule.cashback(), cumulativeWeight));
        }
        this.tiles = List.copyOf(weightedTiles);
        this.totalWeight = cumulativeWeight;
    }

    public Board spawn(Board board, RandomGenerator random) {
        Objects.requireNonNull(board, "Поле не должно быть null");
        Objects.requireNonNull(random, "Генератор случайных чисел не должен быть null");

        var positions = freePositions(board);
        if (positions.isEmpty()) {
            throw new IllegalStateException("Нельзя создать плитку: на поле нет свободных клеток");
        }
        var position = positions.get(random.nextInt(positions.size()));
        var cashback = chooseCashback(random);
        return board.withTile(position, cashback);
    }

    private Cashback chooseCashback(RandomGenerator random) {
        var ticket = nextTicket(random);
        for (var tile : tiles) {
            if (ticket.compareTo(tile.upperBound()) < 0) {
                return tile.cashback();
            }
        }
        throw new IllegalStateException("Не удалось выбрать плитку по весам кампании");
    }

    /** Выбор целого числа в диапазоне [0, totalWeight) с отбрасыванием лишних значений. */
    private BigInteger nextTicket(RandomGenerator random) {
        int bitLength = totalWeight.subtract(BigInteger.ONE).bitLength();
        var source = Random.from(random);
        BigInteger ticket;
        do {
            ticket = new BigInteger(bitLength, source);
        } while (ticket.compareTo(totalWeight) >= 0);

        return ticket;
    }

    private static List<Position> freePositions(Board board) {
        var positions = new ArrayList<Position>();

        for (int row = 0; row < board.size(); row++) {
            for (int column = 0; column < board.size(); column++) {
                var position = new Position(row, column);
                if (board.tileAt(position).isEmpty()) {
                    positions.add(position);
                }
            }
        }
        return positions;
    }

    private record WeightedTile(Cashback cashback, BigInteger upperBound) {}
}
