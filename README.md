# FishNovel

<!-- Plugin description -->
**FishNovel** 是一个面向 IntelliJ IDEA 的摸鱼阅读插件，目标是在 IDE 里提供低打扰、可持续记忆的小说阅读体验。当前版本支持本地 `TXT`、`EPUB`、`Markdown`、`HTML` 四种格式，以及通过网页 URL 在线阅读章节内容；阅读入口统一收口为 `Tool Window`，并保留阅读进度、书签、最近阅读和样式记忆。
<!-- Plugin description end -->

## 当前功能

- 导入本地小说：`TXT / EPUB / Markdown / HTML`
- 在线阅读网页章节
- 统一在工具窗口内阅读
- 自动记住阅读进度、书签、最近阅读
- 支持章节切换、滚动恢复、字号、行距、主题切换
- 支持从书架删除书籍、从书签列表删除书签

## 使用说明

1. 在 IDEA 中打开 `Tools -> 打开 FishNovel`
2. 点击 `导入小说` 选择本地文件，或点击 `在线阅读` 输入章节 URL
3. 在左侧 `书架 / 最近 / 书签` 之间切换
4. 阅读时可以切换章节、调整字号与行距、添加书签
5. 关闭 IDEA 后再次打开，会恢复到上次阅读位置

## 在线阅读提示

- 插件会优先抓取正文容器，并自动跟随 `下一页 / 下一章`
- 如果目标站点把请求重定向到其他域名，插件会直接提示失败，而不是展示空白内容
- 是否能在线阅读成功，取决于目标站点是否实际返回小说正文

## 本地开发

### 环境要求

- IntelliJ IDEA 2025.3
- JDK 21
- 使用仓库内置的 Gradle Wrapper

### 常用命令

```powershell
.\gradlew.bat unitTest
.\gradlew.bat runIde
.\gradlew.bat buildPlugin
```

## 项目结构

- `src/main/java/com/fishnovel/idea/model`: 阅读领域模型
- `src/main/java/com/fishnovel/idea/parser`: 多格式解析器与网页抓取器
- `src/main/java/com/fishnovel/idea/service`: 阅读状态持久化与项目服务
- `src/main/java/com/fishnovel/idea/ui`: 工具窗口与阅读面板
- `src/main/java/com/fishnovel/idea/source`: 远程来源接入协议
- `src/test/java`: 解析器与状态逻辑测试

## 提交到 Gitee

```powershell
git add .
git commit -m "refine FishNovel reader workflow"
git push origin master
```
