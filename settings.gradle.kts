import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "FishNovel"

pluginManagement {
    plugins {
        id("org.jetbrains.intellij.platform") version "2.14.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.14.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}
