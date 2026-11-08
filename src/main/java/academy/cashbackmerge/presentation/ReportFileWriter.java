package academy.cashbackmerge.presentation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Сохранение отчёта в UTF-8 с заменой через временный файл. */
public final class ReportFileWriter {

    public void write(Path path, String content) throws IOException {
        var target = Objects.requireNonNull(path, "Путь отчёта не должен быть null")
                .toAbsolutePath()
                .normalize();
        Objects.requireNonNull(content, "Содержимое отчёта не должно быть null");
        var directory = target.getParent();
        if (directory == null) {
            throw new IOException("Путь отчёта должен указывать на файл: " + target);
        }

        var temporary = Files.createTempFile(directory, "cashback-merge-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
