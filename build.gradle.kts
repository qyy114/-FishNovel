import java.util.Properties

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = "com.fishnovel.idea"
version = "1.0.8"

val signingProperties = Properties().apply {
    val file = layout.projectDirectory.file("certificates/signing.local.properties").asFile
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun localSecret(name: String) = providers.gradleProperty(name)
    .orElse(providers.environmentVariable(name))
    .orElse(signingProperties.getProperty(name) ?: "")

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
        }
        description = """
            FishNovel turns IntelliJ IDEA into a lightweight novel reader with local multi-format support,
            tool-window reading, progress memory, bookmarks, history, and web novel reading.
        """.trimIndent()
        changeNotes = """
            <ul>
                <li>1.0.8: Refresh the reader toolbar layout when the sidebar is collapsed or expanded to avoid stale top whitespace.</li>
                <li>1.0.7: Open the sidebar and reader toolbars by default so the full navigation is visible on first launch.</li>
                <li>1.0.6: Replaced the hidden split pane with fixed sidebar layout so the navigation panel opens at full width on first display.</li>
                <li>1.0.5: Fixed the sidebar's first expanded layout by syncing the split pane after the tool window is shown and resized.</li>
                <li>1.0.4: Reset the tool-window layout with a cleaner sidebar switcher and refined compact reader controls.</li>
                <li>1.0.3: Added automatic UI localization with English fallback and Simplified Chinese translations.</li>
                <li>1.0.2: Refined the collapsed reader toolbar and sidebar toggle layout for a cleaner reading surface.</li>
                <li>1.0.1: Fixed previous and next chapter navigation visibility and behavior.</li>
                <li>Initial 1.0.0 release with local TXT, EPUB, Markdown, and HTML reading.</li>
                <li>Added web chapter reading, progress memory, bookmarks, history, and theme preferences.</li>
                <li>Added Tomato TXT cache import support.</li>
            </ul>
        """.trimIndent()
        vendor {
            name = "FishNovel"
            url = "https://gitee.com/qyy114/fish-novelFishNovel"
        }
    }

    publishing {
        token = providers.gradleProperty("intellijPlatformPublishingToken").orElse("")
        channels = listOf("default")
    }

    signing {
        keyStore.set(layout.projectDirectory.file("certificates/fishnovel-signing.p12"))
        keyStorePassword.set(localSecret("intellijPlatformSigningPassword"))
        keyStoreKeyAlias.set("fishnovel")
        keyStoreType.set("PKCS12")
        certificateChainFile.set(layout.projectDirectory.file("certificates/fishnovel-chain.crt"))
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
        "com.fishnovel.idea.FishNovelBundleTest",
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
