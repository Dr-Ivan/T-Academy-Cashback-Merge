package academy.cashbackmerge.presentation;

import academy.cashbackmerge.model.GameResult;
import academy.cashbackmerge.model.GameState;
import academy.cashbackmerge.model.GameStatus;
import academy.cashbackmerge.model.Move;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Состояние партии, награда и данные для воспроизведения. */
public final class GameSummaryFormatter {

    private final GameStateFormatter stateFormatter = new GameStateFormatter();

    public String format(long seed, GameState state, GameStatus status, List<Move> moves, GameResult result) {
        Objects.requireNonNull(status, "Статус партии не должен быть null");
        Objects.requireNonNull(result, "Результат партии не должен быть null");
        var sequence = Objects.requireNonNull(moves, "Список ходов не должен быть null").stream()
                .map(Move::name)
                .collect(Collectors.joining(","));
        var replayMoves = sequence.isEmpty() ? "(пустой список)" : sequence;

        return "Seed: " + seed + "\nХоды для replay: " + replayMoves + "\n" + stateFormatter.format(state)
                + "Максимальная плитка: " + result.maxCashback().value() + "%\n"
                + status.rewardLabel() + ": " + result.reward().toPlainString() + "\n" + status.message() + "\n";
    }
}
