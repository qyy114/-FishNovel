# FishNovel

FishNovel 是一个 IntelliJ IDEA 小说阅读插件，目标是在 IDE 内提供低干扰、可记忆进度的阅读体验。插件支持本地小说、网页章节阅读、番茄小说 TXT 下载阅读、书架、最近阅读、书签、章节跳转、阅读样式调整和老板键隐藏。

<!-- Plugin description -->
FishNovel turns IntelliJ IDEA into a lightweight novel reader with local multi-format support, web chapter reading, Tomato TXT download reading, progress memory, bookmarks, history, and theme preferences.
<!-- Plugin description end -->

## 主要功能

- 本地阅读：支持 `TXT`、`EPUB`、`Markdown`、`HTML`。
- 网页阅读：输入章节 URL 后读取当前章节，同章分页会自动合并，上一章/下一章按需加载。
- 番茄下载：输入番茄小说 `book_id`、分享链接或详情页链接，调用本机 Tomato-Novel-Downloader 下载 TXT，然后使用 FishNovel 的 TXT 阅读器打开。
- 阅读控制：支持章节下拉、数字跳转、上一章/下一章、刷新、字号、行距、主题和书签。
- 阅读状态：自动保存书架、最近阅读、书签、阅读进度和阅读样式。
- 低干扰界面：工具栏和侧边栏可收起，阅读区不显示小说名，保留紧凑章节进度。
- 老板键：插件打开时按任意键可隐藏工具窗口；关闭状态下不影响 IDE。

## 使用方法

1. 在 IDEA 中打开 `Tools -> 打开 FishNovel`。
2. 展开顶部工具栏。
3. 点击 `导入小说` 选择本地文件，或点击 `在线阅读` 输入网页章节 URL。
4. 点击 `番茄下载` 输入番茄小说 ID 或链接。
5. 首次使用番茄下载时，从官方 Release 下载 `TomatoNovelDownloader-Win64-v2.4.7.exe` 并在文件选择器中选中它。
6. 下载完成后会自动打开 TXT 阅读；后续再次下载不会重复询问 exe 路径。
7. 对番茄书点击 `更新` 时，会重新下载并尽量保留原阅读进度；失败时保留当前正文。

## 本地安装教程

### 方式一：从 IDEA 界面安装

1. 运行 `.\gradlew.bat buildPlugin --console=plain`。
2. 找到插件包：`build/distributions/FishNovel-1.0.11.zip`。
3. 打开 IntelliJ IDEA。
4. 进入 `Settings -> Plugins`。
5. 点击插件页右上角齿轮按钮，选择 `Install Plugin from Disk...`。
6. 选择 `FishNovel-1.0.11.zip`。
7. 按提示重启 IDEA。
8. 重启后在 `Tools -> 打开 FishNovel` 打开插件。

### 方式二：手动复制到 IDEA 插件目录

适合需要直接覆盖本机插件目录的情况。执行前建议关闭 IDEA，避免 Windows 锁住旧版 jar。

```powershell
.\gradlew.bat buildPlugin --console=plain

$pluginRoot = "$env:APPDATA\JetBrains\IntelliJIdea2025.3\plugins"
$zipPath = Join-Path (Get-Location) "build\distributions\FishNovel-1.0.11.zip"
$target = Join-Path $pluginRoot "FishNovel"
$backup = Join-Path $pluginRoot ("FishNovel.backup-" + (Get-Date -Format "yyyyMMdd-HHmmss"))

if (Test-Path $target) {
    Move-Item -LiteralPath $target -Destination $backup
}

Expand-Archive -LiteralPath $zipPath -DestinationPath $pluginRoot -Force
```

安装完成后重新打开 IDEA。首次使用 `番茄下载` 时，如果还没有保存下载器路径，请先从官方 Release 下载 `TomatoNovelDownloader-Win64-v2.4.7.exe`，然后在文件选择器中选中该 exe。

## 番茄下载说明

FishNovel 不内置番茄小说隐藏接口，也不绕过登录、验证码、付费或访问限制。番茄正文下载由第三方工具 Tomato-Novel-Downloader 完成，FishNovel 只负责启动本机下载器、等待 TXT 生成、打开缓存 TXT。

番茄下载器获取方式：

- 来源项目：`zhongbai2333/Tomato-Novel-Downloader`
- 来源地址：https://github.com/zhongbai2333/Tomato-Novel-Downloader/releases/tag/v2.4.7
- 下载资产：`TomatoNovelDownloader-Win64-v2.4.7.exe`

资源占用策略：

- 打开 FishNovel 不会自动启动番茄下载器。
- 只有点击 `番茄下载` 或对番茄书点击 `更新` 时才会启动下载器。
- 下载成功、失败或超时后，FishNovel 会关闭本次启动的番茄下载器进程。
- 从书架重新打开已缓存的番茄 TXT，不会启动下载器。

## 网页阅读说明

- 输入网页章节 URL 后，插件只加载当前章节。
- 如果当前章节有同章分页，插件会自动合并分页内容。
- 上一章/下一章只在点击时加载，不会在导入时一次性抓取整本书。
- 当前内置 `Sudugu` 站点适配器、`BqgAjax` 站点适配器和通用 HTML 适配器。
- 如果目标站点重定向到其他域名、返回空白、需要登录或有验证码，插件会提示失败并保留当前已打开内容。

## 数据位置

FishNovel 没有独立数据库，使用 IDEA 的持久化 XML 和插件缓存目录。

- 阅读状态：`%APPDATA%/JetBrains/IntelliJIdea2025.3/options/fishNovel.xml`
- 番茄缓存：`%LOCALAPPDATA%/JetBrains/IntelliJIdea2025.3/FishNovel/tomato`
- 番茄 TXT：`%LOCALAPPDATA%/JetBrains/IntelliJIdea2025.3/FishNovel/tomato/library`
- 番茄映射：`%LOCALAPPDATA%/JetBrains/IntelliJIdea2025.3/FishNovel/tomato/fishnovel-tomato-books.json`
- 番茄日志：`%LOCALAPPDATA%/JetBrains/IntelliJIdea2025.3/FishNovel/tomato/tomato-downloader.log`

## 开发与验证

环境要求：

- IntelliJ IDEA 2025.3
- JDK 21
- 使用仓库内置 Gradle Wrapper

常用命令：

```powershell
.\gradlew.bat unitTest --console=plain
.\gradlew.bat buildPlugin --console=plain
```

安装产物：

```text
build/distributions/FishNovel-1.0.11.zip
```

## 项目结构

- `src/main/java/com/fishnovel/idea/model`：阅读领域模型。
- `src/main/java/com/fishnovel/idea/parser`：本地文件解析器与网页章节解析。
- `src/main/java/com/fishnovel/idea/service`：阅读状态、项目服务、番茄下载器管理。
- `src/main/java/com/fishnovel/idea/source`：远程书源与番茄来源类型。
- `src/main/java/com/fishnovel/idea/ui`：工具窗口、阅读面板和侧边栏。
- `src/test/java`：解析、书源、跳转、状态和番茄下载服务测试。
