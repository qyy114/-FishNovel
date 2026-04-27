plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = "com.fishnovel.idea"
version = "1.0.0"

dependencies {
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("org.commonmark:commonmark:0.24.0")
    implementation("com.positiondev.epublib:epublib-core:3.1")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        local("D:/develop/IntelliJ IDEA 2025.3.4")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "FishNovel"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "253"
            untilBuild = "253.*"
        }
        description = """
            FishNovel turns IntelliJ IDEA into a discreet novel reader with local multi-format support,
            tool-window reading, progress memory, bookmarks, history, and web novel reading.
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
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
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
        "com.fishnovel.idea.parser.RemoteHtmlBookCrawlerTest",
        "com.fishnovel.idea.parser.TxtBookParserTest",
        "com.fishnovel.idea.source.BqgAjaxSourceAdapterTest",
        "com.fishnovel.idea.source.RemoteChapterSourceRegistryTest",
        "com.fishnovel.idea.service.ChapterJumpResolverTest",
        "com.fishnovel.idea.service.ReadingStateServiceTest",
        "com.fishnovel.idea.service.ReadingProgressResolverTest",
        "com.fishnovel.idea.service.FishNovelProjectServiceTest",
        "com.fishnovel.idea.source.TomatoSourceLocationTest",
        "com.fishnovel.idea.service.TomatoDownloaderServiceTest"
    )
}

tasks.check {
    dependsOn("unitTest")
}
