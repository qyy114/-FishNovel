package com.fishnovel.idea.service;

import com.fishnovel.idea.editor.FishNovelVirtualFile;
import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.BookShelfItem;
import com.fishnovel.idea.model.ReadingProgress;
import com.fishnovel.idea.parser.BookParserRegistry;
import com.fishnovel.idea.source.BookSourceAdapter;
import com.fishnovel.idea.source.FanqiePlaceholderAdapter;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class FishNovelProjectService {
    private final Project project;
    private final BookParserRegistry parserRegistry;
    private final List<BookSourceAdapter> remoteSources;

    public FishNovelProjectService(Project project) {
        this.project = project;
        this.parserRegistry = BookParserRegistry.defaultRegistry();
        this.remoteSources = List.of(new FanqiePlaceholderAdapter());
    }

    public BookDocument importBook(Path path) throws IOException {
        BookDocument document = parserRegistry.parse(path);
        ReadingStateService.getInstance().registerBook(document);
        return document;
    }

    public BookDocument reopen(BookShelfItem item) throws IOException {
        Path path = Path.of(item.getSourceLocation());
        if (!Files.exists(path)) {
            throw new IOException("Book file not found: " + item.getSourceLocation());
        }
        return importBook(path);
    }

    public void openInEditor(BookDocument document) {
        openInEditor(document, null);
    }

    public void openInEditor(BookDocument document, ReadingProgress progress) {
        FileEditorManager.getInstance(project).openFile(new FishNovelVirtualFile(document, progress), true);
    }

    public List<BookSourceAdapter> getRemoteSources() {
        return remoteSources;
    }

    public static FishNovelProjectService getInstance(Project project) {
        return project.getService(FishNovelProjectService.class);
    }
}
