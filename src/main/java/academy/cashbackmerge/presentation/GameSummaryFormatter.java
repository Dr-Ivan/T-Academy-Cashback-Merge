package academy.cashbackmerge.presentation;

import academy.cashbackmerge.model.GameState;
import academy.cashbackmerge.model.GameStatus;
import academy.cashbackmerge.model.Move;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Итоговое состояние партии со статусом и данными для воспроизведения. */
public final class GameSummaryFormatter {

    private final GameStateFormatter stateFormatter = new GameStateFormatter();

    public String format(long seed, GameState state, GameStatus status, List<Move> moves) {
        Objects.requireNonNull(status, "Статус партии не должен быть null");
        var sequence = Objects.requireNonNull(moves, "Список ходов не должен быть null").stream()
                .map(Move::name)
                .collect(Collectors.joining(","));
        var replayMoves = sequence.isEmpty() ? "(пустой список)" : sequence;
        var message =
                switch (status) {
                    case IN_PROGRESS -> "Партия не завершена.";
                    case MOVES_LIMIT_REACHED -> "Партия завершена: достигнут лимит ходов.";
                    case NO_VALID_MOVES -> "Партия завершена: нет доступных ходов.";
                };

        return "Seed: " + seed + "\nХоды для replay: " + replayMoves + "\n" + stateFormatter.format(state) + message
                + "\n";
    }
}
