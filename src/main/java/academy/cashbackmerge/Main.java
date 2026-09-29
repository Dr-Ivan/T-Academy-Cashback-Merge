package academy.cashbackmerge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Точка входа в Cashback Merge. */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {}

    public static void main(String[] args) {
        LOG.debug("Аргументы запуска: {}", String.join(" ", args));

        // TODO: разобрать аргументы и запустить нужный режим.
        System.out.println("Cashback Merge — шаблон проекта Т-Академии.");
        System.out.println("Ничего ещё не реализовано. Начни с README.");
    }
}
