package academy.cashbackmerge.cli;

import academy.cashbackmerge.strategy.PlayerStrategy;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public final class PlayerStrategyConverter implements ITypeConverter<PlayerStrategy> {

    @Override
    public PlayerStrategy convert(String value) {
        try {
            return PlayerStrategy.fromCliName(value);
        } catch (IllegalArgumentException exception) {
            throw new TypeConversionException(exception.getMessage());
        }
    }
}
