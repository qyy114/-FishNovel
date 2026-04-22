plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = "com.fishnovel.idea"
version = "0.1.0"

dependencies {
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("org.commonmark:commonmark:0.24.0")
    implementation("com.positiondev.epublib:epublib-core:3.1")

    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        intellijIdeaCommunity("2024.1")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "FishNovel"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "241"
            untilBuild = "241.*"
        }
        description = """
            FishNovel turns IntelliJ IDEA into a discreet novel reader with local multi-format support,
            reading progress memory, bookmarks, history, and a future-ready Fanqie integration layer.
        """.trimIndent()
        vendor {
            name = "FishNovel"
            email = "support@fishnovel.local"
            url = "https://gitee.com/"
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

val testSourceSet = the<org.gradle.api.tasks.SourceSetContainer>().named("test").get()

tasks.test {
    enabled = false
}

tasks.named("instrumentCode") {
    enabled = false
}

tasks.named("instrumentTestCode") {
    enabled = false
}

tasks.register<JavaExec>("unitTest") {
    group = "verification"
    description = "Runs FishNovel JUnit tests without the IntelliJ sandbox test harness."
    classpath = testSourceSet.runtimeClasspath
    mainClass.set("org.junit.runner.JUnitCore")
    args(
        "com.fishnovel.idea.parser.BookParserRegistryTest",
        "com.fishnovel.idea.service.ReadingStateServiceTest"
    )
}

tasks.check {
    dependsOn("unitTest")
}
