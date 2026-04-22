# FishNovel

<!-- Plugin description -->
**FishNovel** 是一个面向 IntelliJ IDEA 的小说阅读插件，目标是在 IDE 中提供低打扰、可持续记忆的摸鱼阅读体验。

一期支持本地 `TXT`、`EPUB`、`Markdown`、`HTML` 四种格式，提供 `Tool Window` 与 `Editor Tab` 两种阅读方式，并支持阅读进度、书签、最近阅读和样式记忆。项目同时预留番茄小说的数据源接口，但不在当前版本内置真实抓取逻辑。
<!-- Plugin description end -->

## 功能清单

- 本地导入小说文件并自动识别格式
- 侧边工具窗口阅读
- 编辑器标签页沉浸式阅读
- 章节切换、滚动恢复、字号/行距/主题切换
- 每本书的阅读进度、书签、最近阅读记录
- 番茄小说接入层占位实现，便于后续扩展

## 本地开发

### 环境要求

- JDK 17
- IntelliJ IDEA 2024.1
- 通过 `./gradlew` 或 `gradlew.bat` 使用 Gradle Wrapper

### 常用命令

```powershell
.\gradlew.bat unitTest
.\gradlew.bat runIde
.\gradlew.bat buildPlugin
```

## 项目结构

- `src/main/java/com/fishnovel/idea/model`: 阅读领域模型
- `src/main/java/com/fishnovel/idea/parser`: 多格式解析器
- `src/main/java/com/fishnovel/idea/service`: 状态持久化与项目服务
- `src/main/java/com/fishnovel/idea/ui`: Tool Window 与阅读面板
- `src/main/java/com/fishnovel/idea/editor`: 自定义编辑器标签页实现
- `src/main/java/com/fishnovel/idea/source`: 番茄小说等远程数据源接入层
- `src/test/java`: 解析器和状态服务测试

## Gitee 提交流程

1. 在 Gitee 创建同名仓库 `FishNovel`
2. 进入项目目录初始化或确认 Git
3. 添加远端 `origin`
4. 推送默认分支

```powershell
git remote add origin <你的-gitee-仓库地址>
git push -u origin master
```
