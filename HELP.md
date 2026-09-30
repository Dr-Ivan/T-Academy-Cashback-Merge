# Работа с шаблоном

## Что установить

Нужен только **JDK 25 или новее** и любая свежая IntelliJ IDEA
(Community Edition хватает). Gradle ставить не нужно — в проекте лежит
`gradlew`, он скачает нужную версию сам.

Проверить версию Java: `java -version`. Если она ниже 25, сборка остановится
с понятным сообщением.

## Запуск

```bash
# Linux и macOS
./gradlew build
./gradlew run

# Windows
.\gradlew.bat build
.\gradlew.bat run
```

Первая сборка занимает пару минут — Gradle качает себя и зависимости.
Дальше секунды.

Дальше команды написаны как `./gradlew`. На Windows подставляй
`.\gradlew.bat`.

## Команды

| Команда | Что делает |
|---|---|
| `./gradlew run` | Запустить программу |
| `./gradlew run --args="..."` | Запустить с аргументами |
| `./gradlew test` | Прогнать тесты |
| `./gradlew check` | Тесты и линтеры — то же, что проверяет CI |
| `./gradlew spotlessApply` | Отформатировать код |
| `./gradlew clean` | Удалить результаты сборки |

Пример запуска симуляции:

```bash
./gradlew run --args="simulate --config config/campaign.example.json --players 1000 --seed 42"
```

## Настройка IDEA

1. Открыть папку проекта — Gradle подхватится сам.
2. **Settings → Build Tools → Gradle → Gradle JVM** — выбрать JDK 25+.
3. **Settings → Build Tools → Gradle → Run tests using → Gradle.**

Третий пункт обязателен: часть тестов запускает программу отдельным процессом
и получает нужные параметры от Gradle.

## Что уже готово в шаблоне

| Что | Где |
|---|---|
| Пример конфига кампании | `config/campaign.example.json` |
| Заготовки обязательных тестов | `src/test/java/.../mandatory/` |
| `CliRunner` — запускает программу из теста | `src/test/java/.../support/` |
| Настроенные линтеры и CI | `config/`, `.gitlab-ci.yml` |
| Шаблоны описаний merge request'ов | `.gitlab/merge_request_templates/` |

Свои классы складывай в `src/main/java/academy/cashbackmerge/`. Как разложить
их по пакетам — решать тебе, это часть задания.

## Проверка кода

```bash
./gradlew check
```

CI запускается автоматически только для merge request'ов. Одна job `check`
собирает проект, запускает линтеры и тесты, сохраняет JAR и отчёты.
При новом коммите устаревшая проверка MR отменяется; отдельного запуска
после слияния в `master` нет.

Полная локальная проверка, как в CI:

```bash
./gradlew --continue assemble check printCoverage
```

Флаг `--continue` позволяет выполнить независимые проверки, даже если одна
из них упала; итоговый статус при ошибках остаётся неуспешным.

Работают пять инструментов:

- **Spotless** — форматирование. Руками отступы не правь, запусти
  `./gradlew spotlessApply`, и он всё сделает сам.
- **Checkstyle** — именование, структура, типичные ошибки. Отчёт:
  `build/reports/checkstyle/main.html`
- **Error Prone** — настоящие баги: `equals` без `hashCode`, сравнение строк через `==`,
  потерянный результат вызова. Работает во время
  компиляции, поэтому ошибки видно сразу в выводе `./gradlew build`.
- **NullAway** — следит за `null`: если метод может вернуть `null`, пометь его
  `@Nullable` из `org.jspecify.annotations`, иначе сборка упадёт. На тестах
  выключен.
- **JaCoCo** — покрытие тестами. Отчёт:
  `build/reports/jacoco/test/html/index.html`

Если правило мешает в конкретном месте, подави его точечно —
`@SuppressWarnings("checkstyle:MagicNumber")` — но не отключай в конфиге:
такая правка видна в merge request'е, и ментор о ней спросит.

## Библиотеки

Все одобренные библиотеки перечислены в `gradle/libs.versions.toml`.
Чтобы подключить, добавь строку в `dependencies` в `build.gradle.kts`:

```kotlin
implementation(libs.picocli)
```

Версию указывать не нужно.

Уже подключены: **picocli** (разбор аргументов, подкоманда `simulate`),
**Jackson** (JSON-конфиг и отчёт), **SLF4J + Logback** (логирование),
**JUnit 5 + AssertJ** (тесты).

**Добавлять библиотеки, которых нет в каталоге, — только по согласованию
с ментором.** Для точной арифметики ничего искать не надо: `BigDecimal`
есть в стандартной библиотеке.

## Как сдавать

Три merge request'а, по одному на этап. При создании MR выбери шаблон описания
в списке **Description → Choose a template** — там уже готов чеклист.

Перед отправкой прогони у себя:

```bash
./gradlew spotlessApply check
```

К MR3 приложи пример отчёта симуляции. Если реализовал что-то из бонусов —
перечисли это в описании MR, иначе баллы не начислятся.

## Если что-то не работает

**Ошибка про версию Java.** Стоит JDK старее 21. IDEA использует свой JDK
из настроек Gradle, а не тот, что в `JAVA_HOME`.

**`./gradlew: Permission denied`** (Linux, macOS) — `chmod +x ./gradlew`

**Кракозябры вместо русских букв в консоли Windows.** В `cmd` выполни
`chcp 65001` перед запуском, в PowerShell —
`[Console]::OutputEncoding = [System.Text.Encoding]::UTF8`.
В IDEA такой проблемы нет.

**Сумма вероятностей `0.85 + 0.15` не равна `1`.** Это не баг Java, а свойство
чисел с плавающей точкой — см. раздел про MR1 выше.

**Тест падает с «Не задано системное свойство academy.cli.classpath».**
Тест запущен в обход Gradle — включи в IDEA
**Settings → Build Tools → Gradle → Run tests using → Gradle**.

**Spotless ругается, а что не так — непонятно.** Не разбирайся, запусти
`./gradlew spotlessApply`.

**Что-то другое.** Приложи к вопросу вывод `./gradlew check --stacktrace`.
