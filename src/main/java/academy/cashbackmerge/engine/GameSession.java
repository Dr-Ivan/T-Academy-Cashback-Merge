package academy.cashbackmerge.engine;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.model.GameState;
import academy.cashbackmerge.model.GameStatus;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.model.MoveResult;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.random.RandomGenerator;

/** Одна партия с текущим состоянием и непрерывной последовательностью случайных чисел. */
public final class GameSession {

    private final long seed;
    private final GameEngine engine;
    private final RandomGenerator random;
    private GameState state;

    public GameSession(CampaignConfig config, long seed) {
        this.seed = seed;
        this.engine = new GameEngine(config);
        this.random = new Random(seed);
        this.state = new GameInitializer(config).initialize(random);
    }

    public long seed() {
        return seed;
    }

    public GameState state() {
        return state;
    }

    public GameStatus status() {
        return engine.status(state);
    }

    public MoveResult applyMove(Move move) {
        var result = engine.applyMove(state, move, random);
        state = result.state();
        return result;
    }

    /** Восстановление партии по seed и попыткам ходов до завершения игры. */
    public static GameSession replay(CampaignConfig config, long seed, List<Move> moves) {
        var sequence = List.copyOf(Objects.requireNonNull(moves, "Список ходов не должен быть null"));
        var session = new GameSession(config, seed);

        for (var move : sequence) {
            var result = session.applyMove(move);
            if (result.status().isFinished()) {
                break;
            }
        }
        return session;
    }
}
