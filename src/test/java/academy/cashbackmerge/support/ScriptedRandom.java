package academy.cashbackmerge.support;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

/** Управляемый генератор для проверки выбора клетки и границ весовых интервалов. */
public final class ScriptedRandom implements RandomGenerator {

    private final int positionIndex;
    private final List<BigInteger> samples;
    private int samplesUsed;

    public ScriptedRandom(int positionIndex, BigInteger... samples) {
        this.positionIndex = positionIndex;
        this.samples = List.of(samples);
    }

    public int samplesUsed() {
        return samplesUsed;
    }

    @Override
    public int nextInt(int bound) {
        if (positionIndex < 0 || positionIndex >= bound) {
            throw new AssertionError("Индекс клетки вне диапазона генератора: " + positionIndex);
        }
        return positionIndex;
    }

    @Override
    public void nextBytes(byte[] bytes) {
        if (samplesUsed >= samples.size()) {
            throw new AssertionError("Неожиданный запрос случайных байтов");
        }
        var source = samples.get(samplesUsed++).toByteArray();
        Arrays.fill(bytes, (byte) 0);
        int length = Math.min(source.length, bytes.length);
        System.arraycopy(source, source.length - length, bytes, bytes.length - length, length);
    }

    @Override
    public long nextLong() {
        throw new AssertionError("Неожиданный вызов nextLong");
    }
}
