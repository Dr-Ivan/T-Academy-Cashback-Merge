package academy.cashbackmerge.engine;

import static org.assertj.core.api.Assertions.assertThat;

import academy.cashbackmerge.config.CampaignConfig;
import academy.cashbackmerge.config.SpawnRule;
import academy.cashbackmerge.model.Cashback;
import academy.cashbackmerge.model.GameStatus;
import academy.cashbackmerge.model.Move;
import academy.cashbackmerge.model.MoveResult;
import academy.cashbackmerge.support.CampaignFixtures;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Управление партией и воспроизведение ходов")
class GameSessionTest {

    private static final List<Move> CYCLE = List.of(Move.UP, Move.LEFT, Move.DOWN, Move.RIGHT);
    private static final int ATTEMPTS = 64;

    @ParameterizedTest(name = "Seed {0}")
    @ValueSource(longs = {0, 42, -42, Long.MAX_VALUE})
    @DisplayName("Сессия использует один RNG непрерывно для инициализации и последующих ходов")
    void followsContinuousRandomSequence(long seed) {
        var config = CampaignFixtures.validConfig();
        var session = new GameSession(config, seed);
        var random = new Random(seed);
        var reference = new GameInitializer(config).initialize(random);
        var engine = new GameEngine(config);

        assertThat(session.seed()).isEqualTo(seed);
        assertThat(session.state()).isEqualTo(reference);

        for (int step = 0; step < ATTEMPTS; step++) {
            var before = session.state();
            assertThat(session.status()).isEqualTo(engine.status(reference));
            assertThat(session.state()).isSameAs(before);

            var move = CYCLE.get(step % CYCLE.size());
            var expected = engine.applyMove(reference, move, random);
            var actual = session.applyMove(move);

            assertThat(actual)
                    .as("Seed %s, шаг %s, направление %s", seed, step, move)
                    .isEqualTo(expected);
            assertThat(session.state()).isSameAs(actual.state());
            assertThat(session.status()).isEqualTo(actual.status());
            reference = expected.state();

            if (actual.status().isFinished()) {
                break;
            }
        }
    }

    @Test
    @DisplayName("Ходы одной сессии не влияют на состояние и RNG другой сессии")
    void keepsSessionsIndependent() {
        var config = CampaignFixtures.validConfig();
        var first = new GameSession(config, 42);
        var second = new GameSession(config, 42);
        var untouched = second.state();
        var expected = new ArrayList<MoveResult>();

        for (var move : CYCLE) {
            expected.add(first.applyMove(move));
        }
        assertThat(second.state()).isSameAs(untouched);

        for (int index = 0; index < CYCLE.size(); index++) {
            assertThat(second.applyMove(CYCLE.get(index))).isEqualTo(expected.get(index));
        }
    }

    @Test
    @DisplayName("Replay учитывает невалидные попытки и воспроизводит последующие валидные ходы")
    void replaysInvalidAndValidAttempts() {
        var config = CampaignFixtures.validConfig();
        var moves = List.of(Move.RIGHT, Move.LEFT, Move.DOWN, Move.UP, Move.RIGHT);
        var manual = new GameSession(config, 42);
        var initial = manual.state();

        var invalid = manual.applyMove(moves.getFirst());
        assertThat(invalid.changed()).isFalse();
        assertThat(manual.state()).isSameAs(initial);

        for (var move : moves.subList(1, moves.size())) {
            manual.applyMove(move);
        }
        var replayed = GameSession.replay(config, 42, moves);

        assertThat(replayed.seed()).isEqualTo(42);
        assertThat(replayed.state()).isEqualTo(manual.state());
        assertThat(replayed.status()).isEqualTo(manual.status());
        assertThat(replayed.state().movesUsed()).isPositive();
        assertThat(replayed.applyMove(Move.LEFT)).isEqualTo(manual.applyMove(Move.LEFT));
    }

    @Test
    @DisplayName("Пустой список replay восстанавливает начальное поле заданного размера")
    void replaysEmptySequenceAsInitialState() {
        var base = CampaignFixtures.validConfig();
        var config = new CampaignConfig(
                5, 7, base.spawnRules(), base.rewardTable(), base.maxRewardCashback(), base.maxRewardAllowedRate());

        var session = GameSession.replay(config, -42, List.of());

        assertThat(session.seed()).isEqualTo(-42);
        assertThat(session.state()).isEqualTo(new GameInitializer(config).initialize(-42));
        assertThat(session.state().board().size()).isEqualTo(5);
        assertThat(session.state().movesUsed()).isZero();
        assertThat(session.status()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Replay прекращается при достижении лимита, дальнейшая попытка сохраняет состояние")
    void stopsReplayAtMovesLimit() {
        var base = CampaignFixtures.validConfig();
        var config = new CampaignConfig(
                4, 1, base.spawnRules(), base.rewardTable(), base.maxRewardCashback(), base.maxRewardAllowedRate());
        var moves = List.of(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN);

        var session = GameSession.replay(config, 42, moves);
        var onlyFirst = GameSession.replay(config, 42, List.of(Move.LEFT));
        var finished = session.state();
        var afterFinish = session.applyMove(Move.DOWN);

        assertThat(session.state()).isEqualTo(onlyFirst.state());
        assertThat(session.state().movesUsed()).isEqualTo(1);
        assertThat(session.state().board().tiles()).hasSize(3);
        assertThat(session.status()).isEqualTo(GameStatus.MOVES_LIMIT_REACHED);
        assertThat(afterFinish.state()).isSameAs(finished);
        assertThat(session.state()).isSameAs(finished);
        assertThat(afterFinish.changed()).isFalse();
        assertThat(afterFinish.scoreDelta()).isZero();
    }

    @Test
    @DisplayName("Replay завершается при отсутствии ходов, даже если лимит ещё не достигнут")
    void stopsReplayWhenEveryDirectionIsBlocked() {
        var base = CampaignFixtures.validConfig();
        var config = new CampaignConfig(
                2,
                100,
                List.of(new SpawnRule(Cashback.of(32), BigDecimal.ONE)),
                base.rewardTable(),
                base.maxRewardCashback(),
                base.maxRewardAllowedRate());

        var moves = List.of(Move.RIGHT, Move.UP, Move.LEFT, Move.DOWN);
        var session = GameSession.replay(config, 42, moves);
        var finished = session.state();
        var afterFinish = session.applyMove(Move.RIGHT);

        assertThat(session.state().board().tiles()).hasSize(4);
        assertThat(session.state().movesUsed()).isEqualTo(2);
        assertThat(session.state().score()).isZero();
        assertThat(session.status()).isEqualTo(GameStatus.NO_VALID_MOVES);
        assertThat(afterFinish.state()).isSameAs(finished);
        assertThat(session.state()).isSameAs(finished);
        assertThat(afterFinish.changed()).isFalse();
        assertThat(afterFinish.scoreDelta()).isZero();
    }
}
