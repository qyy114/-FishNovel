package com.fishnovel.idea.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class BookDocument {
    private final String bookId;
    private final String title;
    private final SourceType sourceType;
    private final String sourceLocation;
    private final String fileExtension;
    private final String contentHash;
    private final Path sourcePath;
    private final List<Chapter> chapters;

    public BookDocument(
        String bookId,
        String title,
        SourceType sourceType,
        String sourceLocation,
        String fileExtension,
        String contentHash,
        Path sourcePath,
        List<Chapter> chapters
    ) {
        this.bookId = bookId;
        this.title = title;
        this.sourceType = sourceType;
        this.sourceLocation = sourceLocation;
        this.fileExtension = fileExtension;
        this.contentHash = contentHash;
        this.sourcePath = sourcePath;
        this.chapters = List.copyOf(chapters);
    }

    public String getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public List<Chapter> getChapters() {
        return Collections.unmodifiableList(chapters);
    }

    public Chapter getChapter(int index) {
        if (chapters.isEmpty()) {
            throw new IllegalStateException("Book has no chapters");
        }
        int safeIndex = Math.max(0, Math.min(index, chapters.size() - 1));
        return chapters.get(safeIndex);
    }
}
