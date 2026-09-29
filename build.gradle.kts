import java.util.Locale
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    application
    checkstyle
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.errorprone)
}

group = "academy"
version = "1.0.0"

val javaVersion = JavaVersion.VERSION_25
if (JavaVersion.current() < javaVersion) {
    throw GradleException(
        """
        |
        |Нужен JDK $javaVersion или новее, а сборка запущена на JDK ${JavaVersion.current()}.
        |
        |IntelliJ IDEA: Settings -> Build Tools -> Gradle -> Gradle JVM.
        |Терминал: установи JDK 25 и укажи его в JAVA_HOME.
        |
        |Сейчас используется: ${System.getProperty("java.home")}
        """.trimMargin()
    )
}

val utf8JvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")

application {
    mainClass = "academy.cashbackmerge.Main"
    applicationName = "cashback-merge"
    applicationDefaultJvmArgs = utf8JvmArgs
}

tasks.named<JavaExec>("run") {
    // Без этого `./gradlew run` не увидит ввод с клавиатуры.
    standardInput = System.`in`
}

dependencies {
    implementation(libs.picocli)

    implementation(platform(libs.jackson.bom))
    implementation(libs.bundles.jackson)

    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)

    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
    compileOnly(libs.jspecify)
    testCompileOnly(libs.jspecify)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Раскомментируй, если понадобится:
    // testImplementation(platform(libs.mockito.bom))     // моки
    // testImplementation(libs.bundles.mockito)
}

// Проверки Error Prone. Список повторяет соглашения внутренних Gradle-плагинов банка.
val errorProneErrors = listOf(
    "MissingOverride", "BadImport", "MissingCasesInEnumSwitch", "FutureReturnValueIgnored",
    "ReturnValueIgnored", "EqualsIncompatibleType", "FormatStringAnnotation", "ImmutableEnumChecker",
    "CollectionIncompatibleType", "InvalidPatternSyntax", "MustBeClosedChecker", "StreamResourceLeak",
    "GuardedBy", "SynchronizeOnNonFinalField",
)
val errorProneWarnings = listOf(
    "UnnecessaryParentheses", "EmptyBlockTag", "MissingSummary", "UnnecessaryAnonymousClass",
    "PreferJavaTimeOverload", "UnusedVariable", "UnusedMethod",
)
val errorProneOff = listOf("StringCaseLocaleUsage", "PreferDurationOverload", "StringSplitter")

// На тестах эти проверки только мешают: фикстуры и хелперы часто выглядят «неиспользованными».
val errorProneOffInTests = listOf("UnusedVariable", "UnusedMethod", "MissingSummary")

tasks.withType<JavaCompile>().configureEach {
    val isTestCompile = name.startsWith("compileTest")

    options.encoding = "UTF-8"
    options.release = javaVersion.majorVersion.toInt()
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing,-serial", "-parameters"))

    options.errorprone {
        disableWarningsInGeneratedCode.set(true)

        errorProneErrors.forEach { check(it, CheckSeverity.ERROR) }
        errorProneWarnings.forEach { check(it, CheckSeverity.WARN) }
        errorProneOff.forEach { check(it, CheckSeverity.OFF) }

        if (isTestCompile) {
            // NullAway на тестах выключен: в фикстурах null подставляют намеренно.
            disable("NullAway")
            errorProneOffInTests.forEach { check(it, CheckSeverity.OFF) }
        } else {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "academy")
            option("NullAway:JSpecifyMode", "true")
            option("NullAway:AcknowledgeRestrictiveAnnotations", "true")
        }
    }
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(utf8JvmArgs)
    finalizedBy(tasks.jacocoTestReport)

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) = Unit
        override fun beforeTest(test: TestDescriptor) = Unit
        override fun afterTest(test: TestDescriptor, result: TestResult) = Unit
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent != null) return
            logger.lifecycle(
                "\nТесты: ${result.testCount} всего, ${result.successfulTestCount} прошло, " +
                    "${result.failedTestCount} упало, ${result.skippedTestCount} пропущено."
            )
            if (result.skippedTestCount > 0) {
                logger.lifecycle("Пропущенные — обязательные тесты с @Disabled. См. TASK.md\n")
            }
        }
    })

    // Читает CliRunner, чтобы запустить программу отдельным процессом.
    val runtimeClasspath = sourceSets.main.get().runtimeClasspath
    inputs.files(runtimeClasspath).withNormalizer(ClasspathNormalizer::class)
    doFirst {
        systemProperty("academy.cli.classpath", runtimeClasspath.asPath)
        systemProperty("academy.cli.mainClass", application.mainClass.get())
    }
}

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat(libs.versions.palantir.java.format.get())
            .style("PALANTIR")
            .formatJavadoc(true)
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        toggleOffOn()
    }
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = file("config/checkstyle/checkstyle.xml")
    configDirectory = layout.projectDirectory.dir("config/checkstyle")
    isIgnoreFailures = false
    maxWarnings = 0
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required = true
        xml.required = true
        csv.required = true
    }
}

// Строку с процентом покрытия вылавливает регулярка в .gitlab-ci.yml.
tasks.register("printCoverage") {
    dependsOn(tasks.jacocoTestReport)
    val csvReport = layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.csv")
    doLast {
        val file = csvReport.get().asFile
        if (!file.exists()) {
            logger.lifecycle("Отчёт о покрытии не найден — тесты не запускались.")
            return@doLast
        }
        // Колонки: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,...
        var missed = 0L
        var covered = 0L
        file.readLines().drop(1).filter { it.isNotBlank() }.forEach {
            val columns = it.split(",")
            missed += columns[3].toLong()
            covered += columns[4].toLong()
        }
        val total = missed + covered
        val percent = if (total == 0L) 0.0 else covered * 100.0 / total
        // Locale.ROOT: иначе «0,0%» с запятой, и регулярка в CI не совпадёт.
        logger.lifecycle(String.format(Locale.ROOT, "Покрытие инструкций: %.1f%%", percent))
    }
}

// Включается флагом: ./gradlew check -PcoverageGate=true
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.50".toBigDecimal()
            }
        }
    }
}

if (providers.gradleProperty("coverageGate").orNull == "true") {
    tasks.check {
        dependsOn(tasks.jacocoTestCoverageVerification)
    }
}
