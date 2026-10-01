package academy.cashbackmerge.config;

/** Ошибка чтения, формата или содержимого конфигурации кампании. */
public final class ConfigLoadException extends RuntimeException {

    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
