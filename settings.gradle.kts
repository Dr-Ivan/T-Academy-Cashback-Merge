pluginManagement {
    repositories {
        maven("https://mvn-mirror.gitverse.ru")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.diffplug.spotless" ->
                    useModule("com.diffplug.spotless:spotless-plugin-gradle:${requested.version}")
                "net.ltgt.errorprone" ->
                    useModule("net.ltgt.gradle:gradle-errorprone-plugin:${requested.version}")
            }
        }
    }
}

rootProject.name = "homework-2-cashback-merge"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        maven("https://mvn-mirror.gitverse.ru")
    }
}
