package com.fishnovel.idea.model;

public final class BookShelfItem {
    private final String bookId;
    private final String title;
    private final String sourceLocation;
    private final SourceType sourceType;
    private final long lastOpenedAt;
    private final int bookmarkCount;

    public BookShelfItem(String bookId, String title, String sourceLocation, SourceType sourceType, long lastOpenedAt, int bookmarkCount) {
        this.bookId = bookId;
        this.title = title;
        this.sourceLocation = sourceLocation;
        this.sourceType = sourceType;
        this.lastOpenedAt = lastOpenedAt;
        this.bookmarkCount = bookmarkCount;
    }

    public String getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public long getLastOpenedAt() {
        return lastOpenedAt;
    }

    public int getBookmarkCount() {
        return bookmarkCount;
    }

    @Override
    public String toString() {
        return title;
    }
}
