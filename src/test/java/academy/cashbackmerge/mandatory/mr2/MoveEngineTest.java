package academy.cashbackmerge.mandatory.mr2;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.engine.GameEngine;
import academy.cashbackmerge.model.Board;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.GameState;
import academy.cashbackmerge.model.GameStatus;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.model.Position;
import academy.cashbackmerge.support.CampaignFixtures;
import academy.cashbackmerge.support.ScriptedRandom;
import java.math.BigInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: движок ходов — что считается ходом и что после него происходит. */
@DisplayName("MR2. Движок ходов")
class MoveEngineTest {

    private final GameEngine engine = new GameEngine(CampaignFixtures.validConfig());

    @Test
    @DisplayName("Невалидный ход не меняет поле, не тратит ход и не создаёт плитку")
    void invalidMoveChangesNothing() {
        var board = Board.empty(4)
                .withTile(new Position(0, 0), Cashback.of(1))
                .withTile(new Position(0, 1), Cashback.of(2));
        var state = new GameState(board, 7, 2, 10);

        var result = engine.applyMove(state, Move.LEFT, new ScriptedRandom(-1));

        assertThat(result.state()).isSameAs(state);
        assertThat(result.changed()).isFalse();
        assertThat(result.scoreDelta()).isZero();
        assertThat(result.status()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("После валидного хода появляется ровно одна новая плитка")
    void validMoveSpawnsExactlyOneTile() {
        var board = Board.empty(4)
                .withTile(new Position(0, 1), Cashback.of(1))
                .withTile(new Position(0, 3), Cashback.of(2));
        var state = new GameState(board, 7, 0, 0);
        var random = new ScriptedRandom(0, BigInteger.ZERO);
        var expected = Board.empty(4)
                .withTile(new Position(0, 0), Cashback.of(1))
                .withTile(new Position(0, 1), Cashback.of(2))
                .withTile(new Position(0, 2), Cashback.of(1));

        var result = engine.applyMove(state, Move.LEFT, random);

        assertThat(result.state().board()).isEqualTo(expected);
        assertThat(result.state().movesUsed()).isEqualTo(1);
        assertThat(result.state().score()).isZero();
        assertThat(result.changed()).isTrue();
        assertThat(result.scoreDelta()).isZero();
        assertThat(result.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(random.samplesUsed()).isEqualTo(1);
        assertThat(board.tiles()).hasSize(2);
    }

    @Test
    @DisplayName("После N валидных ходов игра завершена")
    void gameEndsAfterMovesLimit() {
        var board = Board.empty(4)
                .withTile(new Position(0, 1), Cashback.of(1))
                .withTile(new Position(0, 3), Cashback.of(2));
        var state = new GameState(board, 2, 0, 0);
        var random = new ScriptedRandom(0, BigInteger.ZERO, BigInteger.ZERO);

        var first = engine.applyMove(state, Move.LEFT, random);
        var last = engine.applyMove(first.state(), Move.DOWN, random);
        var afterFinish = engine.applyMove(last.state(), Move.UP, new ScriptedRandom(-1));

        assertThat(first.changed()).isTrue();
        assertThat(first.state().movesUsed()).isEqualTo(1);
        assertThat(first.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(last.changed()).isTrue();
        assertThat(last.state().movesUsed()).isEqualTo(2);
        assertThat(last.state().board().tiles()).hasSize(4);
        assertThat(last.status()).isEqualTo(GameStatus.MOVES_LIMIT_REACHED);
        assertThat(engine.status(last.state())).isEqualTo(GameStatus.MOVES_LIMIT_REACHED);
        assertThat(random.samplesUsed()).isEqualTo(2);
        assertThat(afterFinish.state()).isSameAs(last.state());
        assertThat(afterFinish.changed()).isFalse();
        assertThat(afterFinish.scoreDelta()).isZero();
        assertThat(afterFinish.status()).isEqualTo(GameStatus.MOVES_LIMIT_REACHED);
    }

    @Test
    @DisplayName("Достижение плитки 32% не заканчивает игру — доигрываем до лимита ходов")
    void reachingMaxTileDoesNotEndGame() {
        var board = Board.empty(4)
                .withTile(new Position(0, 0), Cashback.of(16))
                .withTile(new Position(0, 1), Cashback.of(16));
        var state = new GameState(board, 7, 0, 0);
        var random = new ScriptedRandom(0, BigInteger.ZERO, BigInteger.ZERO);

        var merged = engine.applyMove(state, Move.LEFT, random);
        var continued = engine.applyMove(merged.state(), Move.RIGHT, random);

        assertThat(merged.state().board().tileAt(new Position(0, 0))).contains(Cashback.of(32));
        assertThat(merged.scoreDelta()).isEqualTo(32);
        assertThat(merged.state().score()).isEqualTo(32);
        assertThat(merged.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(continued.changed()).isTrue();
        assertThat(continued.state().movesUsed()).isEqualTo(2);
        assertThat(continued.state().score()).isEqualTo(32);
        assertThat(continued.scoreDelta()).isZero();
        assertThat(continued.status()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Score считается по правилам и растёт только при слияниях")
    void scoreIsCalculatedOnMerges() {
        var board = Board.empty(4)
                .withTile(new Position(0, 0), Cashback.of(2))
                .withTile(new Position(0, 1), Cashback.of(2))
                .withTile(new Position(0, 2), Cashback.of(4))
                .withTile(new Position(0, 3), Cashback.of(4));
        var state = new GameState(board, 7, 2, 100);
        var random = new ScriptedRandom(0, BigInteger.ZERO, BigInteger.ZERO);

        var merged = engine.applyMove(state, Move.LEFT, random);
        var shifted = engine.applyMove(merged.state(), Move.DOWN, random);

        assertThat(merged.scoreDelta()).isEqualTo(12);
        assertThat(merged.state().score()).isEqualTo(112);
        assertThat(merged.state().movesUsed()).isEqualTo(3);
        assertThat(merged.state().board().tileAt(new Position(0, 0))).contains(Cashback.of(4));
        assertThat(merged.state().board().tileAt(new Position(0, 1))).contains(Cashback.of(8));
        assertThat(shifted.changed()).isTrue();
        assertThat(shifted.scoreDelta()).isZero();
        assertThat(shifted.state().score()).isEqualTo(112);
        assertThat(shifted.state().movesUsed()).isEqualTo(4);
    }
}
