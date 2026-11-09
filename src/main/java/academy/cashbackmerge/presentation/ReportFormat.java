package academy.cashbackmerge.presentation;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public enum ReportFormat {
    MARKDOWN(".md") {
        @Override
        String document(String title, String body) {
            return "# " + escape(title) + "\n\n" + body;
        }

        @Override
        String heading(String text) {
            return "## " + escape(text) + "\n\n";
        }

        @Override
        String subheading(String text) {
            return "### " + escape(text) + "\n\n";
        }

        @Override
        String paragraph(String text) {
            return escape(text) + "\n\n";
        }

        @Override
        String table(List<String> headers, List<List<String>> rows) {
            var output = new StringBuilder();
            appendRow(output, headers);
            appendRow(output, Collections.nCopies(headers.size(), "---"));
            for (var row : rows) {
                appendRow(output, row);
            }
            return output.append('\n').toString();
        }

        private void appendRow(StringBuilder output, List<String> cells) {
            output.append('|');
            for (var cell : cells) {
                output.append(' ').append(escape(cell)).append(" |");
            }
            output.append('\n');
        }

        private String escape(String text) {
            return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\\", "\\\\")
                    .replace("|", "\\|")
                    .replace("*", "\\*")
                    .replace("_", "\\_")
                    .replace("`", "\\`")
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                    .replace("\n", "<br>");
        }
    },
    HTML(".html") {
        @Override
        String document(String title, String body) {
            return """
                    <!DOCTYPE html>
                    <html lang="ru">
                    <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>%s</title>
                    <style>
                    body { font-family: system-ui, sans-serif; max-width: 960px; margin: 2rem auto;
                           padding: 0 1rem; color: #17202a; line-height: 1.5; }
                    table { border-collapse: collapse; width: 100%%; margin: 1rem 0 1.5rem; }
                    th, td { border: 1px solid #ccd1d1; padding: .5rem .75rem;
                             text-align: left; overflow-wrap: anywhere; }
                    th { background: #f2f4f4; }
                    tbody tr:nth-child(even) { background: #fafafa; }
                    </style>
                    </head>
                    <body>
                    <main>
                    <h1>%s</h1>
                    %s</main>
                    </body>
                    </html>
                    """.formatted(escape(title), escape(title), body);
        }

        @Override
        String heading(String text) {
            return "<h2>" + escape(text) + "</h2>\n";
        }

        @Override
        String subheading(String text) {
            return "<h3>" + escape(text) + "</h3>\n";
        }

        @Override
        String paragraph(String text) {
            return "<p>" + escape(text) + "</p>\n";
        }

        @Override
        String table(List<String> headers, List<List<String>> rows) {
            var output = new StringBuilder("<table>\n<thead><tr>");
            for (var header : headers) {
                output.append("<th scope=\"col\">").append(escape(header)).append("</th>");
            }
            output.append("</tr></thead>\n<tbody>\n");
            for (var row : rows) {
                output.append("<tr>");
                for (var cell : row) {
                    output.append("<td>").append(escape(cell)).append("</td>");
                }
                output.append("</tr>\n");
            }
            return output.append("</tbody>\n</table>\n").toString();
        }

        private String escape(String text) {
            return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    };

    private final String extension;

    ReportFormat(String extension) {
        this.extension = extension;
    }

    public static ReportFormat fromPath(Path path) {
        var fileName =
                Objects.requireNonNull(path, "Путь отчёта не должен быть null").getFileName();
        if (fileName != null) {
            var name = fileName.toString().toLowerCase(Locale.ROOT);
            for (var format : values()) {
                if (name.endsWith(format.extension)) {
                    return format;
                }
            }
        }
        throw new IllegalArgumentException("Путь --output должен оканчиваться на .md или .html: " + path);
    }

    abstract String document(String title, String body);

    abstract String heading(String text);

    abstract String subheading(String text);

    abstract String paragraph(String text);

    abstract String table(List<String> headers, List<List<String>> rows);
}
