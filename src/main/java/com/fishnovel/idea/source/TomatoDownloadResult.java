package com.fishnovel.idea.source;

import java.nio.file.Path;

public final class TomatoDownloadResult {
    private final String bookId;
    private final String title;
    private final Path txtPath;

    public TomatoDownloadResult(String bookId, String title, Path txtPath) {
        this.bookId = bookId;
        this.title = title;
        this.txtPath = txtPath;
    }

    public String getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public Path getTxtPath() {
        return txtPath;
    }
}
