# FishNovel

FishNovel is an IntelliJ IDEA plugin that turns the IDE into a quiet, lightweight novel reader. It supports local novels, web chapter reading, Tomato novel TXT downloads, bookshelf history, bookmarks, chapter navigation, reading style preferences, and a quick-hide boss key.

## Repository Links

- GitHub: https://github.com/qyy114/-FishNovel
- Gitee: https://gitee.com/qyy114/fish-novelFishNovel

<!-- Plugin description -->
FishNovel turns IntelliJ IDEA into a lightweight novel reader with local multi-format support, web chapter reading, Tomato TXT download reading, progress memory, bookmarks, history, and theme preferences.
<!-- Plugin description end -->

## Features

- Local reading: supports `TXT`, `EPUB`, `Markdown`, and `HTML`.
- Web reading: open a single chapter URL, merge same-chapter pagination, and load previous or next chapters on demand.
- Tomato downloads: enter a Tomato novel `book_id`, share link, or detail page link. FishNovel starts the bundled Windows x64 Tomato-Novel-Downloader, downloads TXT, and opens it in the reader.
- Reading controls: chapter selector, numbered jump, previous/next chapter, refresh, font size, line spacing, themes, and bookmarks.
- Reading state: persists bookshelf entries, recent reading, bookmarks, progress, and style preferences.
- Low-distraction UI: collapsible toolbar and sidebar, compact chapter progress, and no visible novel title inside the reading area.
- Boss key: when the plugin tool window is open, pressing any key hides it without affecting IDEA when the tool window is closed.

## Usage

1. Open `Tools -> Open FishNovel` in IntelliJ IDEA.
2. Expand the top toolbar.
3. Click `Local Import` to choose a local file, or click `Online Reading` to enter a web chapter URL.
4. Click `Tomato Download` and enter a Tomato novel ID or link.
5. On first Tomato download, FishNovel automatically extracts and starts the bundled `TomatoNovelDownloader-Win64-v2.4.9.exe`. If that bundled version fails, choose an external exe as a fallback.
6. When the download completes, FishNovel opens the generated TXT automatically.
7. For a Tomato book, click `Refresh` to download again while preserving reading progress where possible.

## Local Installation

### Install From IDEA

1. Run `.\gradlew.bat buildPlugin --console=plain`.
2. Find the plugin package at `build/distributions/FishNovel-2.0.0.zip`.
3. Open IntelliJ IDEA.
4. Go to `Settings -> Plugins`.
5. Click the gear button and choose `Install Plugin from Disk...`.
6. Select `FishNovel-2.0.0.zip`.
7. Restart IDEA when prompted.
8. Open the plugin from `Tools -> Open FishNovel`.

### Manual Copy

Use this when you want to replace the locally installed plugin directory directly. Close IDEA first to avoid Windows locking the old jar.

```powershell
.\gradlew.bat buildPlugin --console=plain

$pluginRoot = "$env:APPDATA\JetBrains\IntelliJIdea2025.3\plugins"
$zipPath = Join-Path (Get-Location) "build\distributions\FishNovel-2.0.0.zip"
$target = Join-Path $pluginRoot "FishNovel"
$backup = Join-Path $pluginRoot ("FishNovel.backup-" + (Get-Date -Format "yyyyMMdd-HHmmss"))

if (Test-Path $target) {
    Move-Item -LiteralPath $target -Destination $backup
}

Expand-Archive -LiteralPath $zipPath -DestinationPath $pluginRoot -Force
```

After installation, reopen IDEA. Tomato downloads include a bundled Windows x64 downloader. You only need to choose an external `TomatoNovelDownloader-Win64-*.exe` when the bundled downloader cannot start or when you intentionally want to replace it.

## Tomato Download Notes

FishNovel does not embed hidden Tomato novel APIs and does not bypass login, captcha, payment, or access restrictions. Tomato content download is delegated to the third-party Tomato-Novel-Downloader project. FishNovel only starts the bundled or user-selected local downloader, waits for TXT output, and opens the cached TXT file.

Downloader source:

- Project: `zhongbai2333/Tomato-Novel-Downloader`
- Bundled version: `v2.4.9`
- Source release: https://github.com/zhongbai2333/Tomato-Novel-Downloader/releases/tag/v2.4.9
- Bundled asset: `TomatoNovelDownloader-Win64-v2.4.9.exe`
- License: MIT License, included with the plugin resources.

Resource policy:

- Opening FishNovel does not start the Tomato downloader.
- The downloader starts only when you click `Tomato Download` or refresh a Tomato book.
- FishNovel closes the managed downloader process after success, failure, or timeout.
- Reopening a cached Tomato TXT from the bookshelf does not start the downloader.

## Web Reading Notes

- FishNovel loads only the current chapter URL on import.
- Same-chapter pagination is merged automatically when supported.
- Previous and next chapters are loaded only when clicked.
- Built-in adapters currently include `Sudugu`, `BqgAjax`, and a generic HTML adapter.
- If a target site redirects to another domain, returns blank content, requires login, or shows captcha, FishNovel reports a clear failure and keeps the current content.

## Data Locations

FishNovel does not use a standalone database. It stores state through IDEA persistent XML and plugin cache directories.

- Reading state: `%APPDATA%/JetBrains/IntelliJIdea2025.3/options/fishNovel.xml`
- Tomato cache: `%LOCALAPPDATA%/JetBrains/IntelliJIdea2025.3/FishNovel/tomato`
- Tomato TXT files: `%LOCALAPPDATA%/JetBrains/IntelliJIdea2025.3/FishNovel/tomato/library`
- Tomato mapping file: `%LOCALAPPDATA%/JetBrains/IntelliJIdea2025.3/FishNovel/tomato/fishnovel-tomato-books.json`
- Tomato log file: `%LOCALAPPDATA%/JetBrains/IntelliJIdea2025.3/FishNovel/tomato/tomato-downloader.log`

## Development

Requirements:

- IntelliJ IDEA 2025.3
- JDK 21
- The Gradle wrapper included in this repository

Useful commands:

```powershell
.\gradlew.bat unitTest --console=plain
.\gradlew.bat buildPlugin --console=plain
```

Plugin package:

```text
build/distributions/FishNovel-2.0.0.zip
```

## Project Structure

- `src/main/java/com/fishnovel/idea/model`: reading domain models.
- `src/main/java/com/fishnovel/idea/parser`: local file parsers and web chapter parsing.
- `src/main/java/com/fishnovel/idea/service`: reading state, project services, and Tomato downloader management.
- `src/main/java/com/fishnovel/idea/source`: remote sources and Tomato source types.
- `src/main/java/com/fishnovel/idea/ui`: tool window, reader panel, and sidebar.
- `src/test/java`: parser, source, jump, state, and Tomato downloader service tests.
