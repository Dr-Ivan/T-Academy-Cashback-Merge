package academy.cashbackmerge.cli;

import academy.cashbackmerge.model.Move;
import java.util.Arrays;
import java.util.List;
import picocli.CommandLine.ITypeConverter;

/** Проверенная последовательность попыток ходов из одного аргумента CLI. */
public record MoveSequence(List<Move> moves) {

    public MoveSequence {
        moves = List.copyOf(moves);
    }

    public static final class Converter implements ITypeConverter<MoveSequence> {

        @Override
        public MoveSequence convert(String value) {
            var converter = new MoveConverter();
            var moves =
                    Arrays.stream(value.split(",", -1)).map(converter::convert).toList();
            return new MoveSequence(moves);
        }
    }
}
