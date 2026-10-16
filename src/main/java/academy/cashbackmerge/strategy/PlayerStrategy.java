package academy.cashbackmerge.strategy;

import academy.cashbackmerge.engine.BoardMover;
import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Move;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public enum PlayerStrategy {
    RANDOM("random") {
        @Override
        Move selectMove(List<MoveOption> options, RandomGenerator random) {
            return options.get(random.nextInt(options.size())).move();
        }
    },
    GREEDY_SCORE("greedy-score") {
        @Override
        Move selectMove(List<MoveOption> options, RandomGenerator random) {
            var best = options.getFirst();
            for (var option : options) {
                if (option.scoreDelta() > best.scoreDelta()) {
                    best = option;
                }
            }
            return best.move();
        }
    };

    private static final BoardMover MOVER = new BoardMover();
    private final String cliName;

    PlayerStrategy(String cliName) {
        this.cliName = cliName;
    }

    public String cliName() {
        return cliName;
    }

    public Move chooseMove(Board board, RandomGenerator random) {
        Objects.requireNonNull(board, "Поле не должно быть null");
        Objects.requireNonNull(random, "Генератор случайных чисел не должен быть null");

        var options = new ArrayList<MoveOption>();
        for (var move : Move.values()) {
            var result = MOVER.move(board, move);
            if (result.changed()) {
                options.add(new MoveOption(move, result.scoreDelta()));
            }
        }

        if (options.isEmpty()) {
            throw new IllegalStateException("На поле нет доступных ходов");
        }
        return selectMove(options, random);
    }

    abstract Move selectMove(List<MoveOption> options, RandomGenerator random);

    record MoveOption(Move move, long scoreDelta) {}
}
