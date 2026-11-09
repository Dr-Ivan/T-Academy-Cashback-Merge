package academy.cashbackmerge.engine;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.model.GameState;
import academy.cashbackmerge.model.GameStatus;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.model.MoveResult;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Выполнение хода с начислением очков, появлением плитки и проверкой завершения партии. */
public final class GameEngine {

    private final BoardMover mover = new BoardMover();
    private final TileSpawner spawner;

    public GameEngine(CampaignConfig config) {
        this.spawner = new TileSpawner(config);
    }

    public MoveResult applyMove(GameState state, Move move, RandomGenerator random) {
        Objects.requireNonNull(state, "Состояние игры не должно быть null");
        Objects.requireNonNull(move, "Направление не должно быть null");
        Objects.requireNonNull(random, "Генератор случайных чисел не должен быть null");

        if (state.movesUsed() == state.movesLimit()) {
            return new MoveResult(state, false, 0, GameStatus.MOVES_LIMIT_REACHED);
        }
        var boardMove = mover.move(state.board(), move);
        if (!boardMove.changed()) {
            return new MoveResult(state, false, 0, status(state));
        }
        long score = Math.addExact(state.score(), boardMove.scoreDelta());
        int movesUsed = state.movesUsed() + 1;
        var board = spawner.spawn(boardMove.board(), random);
        var updated = new GameState(board, state.movesLimit(), movesUsed, score);

        return new MoveResult(updated, true, boardMove.scoreDelta(), status(updated));
    }

    public GameStatus status(GameState state) {
        Objects.requireNonNull(state, "Состояние игры не должно быть null");
        if (state.movesUsed() == state.movesLimit()) {
            return GameStatus.MOVES_LIMIT_REACHED;
        }
        for (var move : Move.values()) {
            if (mover.move(state.board(), move).changed()) {
                return GameStatus.IN_PROGRESS;
            }
        }
        return GameStatus.NO_VALID_MOVES;
    }
}
