package academy.cashbackmerge.engine;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.GameState;
import java.util.Objects;
import java.util.Random;
import java.util.random.RandomGenerator;

/** Создание начального состояния партии с двумя плитками. */
public final class GameInitializer {

    private static final int INITIAL_TILE_COUNT = 2;

    private final CampaignConfig config;
    private final TileSpawner spawner;

    public GameInitializer(CampaignConfig config) {
        this.config = Objects.requireNonNull(config, "Конфигурация не должна быть null");
        this.spawner = new TileSpawner(config);
    }

    public GameState initialize(long seed) {
        return initialize(new Random(seed));
    }

    public GameState initialize(RandomGenerator random) {
        Objects.requireNonNull(random, "Генератор случайных чисел не должен быть null");

        var board = Board.empty(config.boardSize());
        for (int index = 0; index < INITIAL_TILE_COUNT; index++) {
            board = spawner.spawn(board, random);
        }
        return new GameState(board, config.movesLimit(), 0, 0);
    }
}
