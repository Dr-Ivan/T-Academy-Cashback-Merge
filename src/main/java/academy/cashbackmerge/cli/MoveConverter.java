package academy.cashbackmerge.cli;

import academy.cashbackmerge.model.Move;
import java.util.Locale;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public final class MoveConverter implements ITypeConverter<Move> {

    @Override
    public Move convert(String value) {
        try {
            return Move.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            throw new TypeConversionException(
                    "Неизвестное направление '" + value + "'. Допустимые значения: UP, DOWN, LEFT, RIGHT.");
        }
    }
}
