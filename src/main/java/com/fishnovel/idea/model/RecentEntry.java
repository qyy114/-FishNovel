package com.fishnovel.idea.model;

public final class RecentEntry {
    private final String bookId;
    private final String title;
    private final String sourceLocation;
    private final long lastOpenedAt;

    public RecentEntry(String bookId, String title, String sourceLocation, long lastOpenedAt) {
        this.bookId = bookId;
        this.title = title;
        this.sourceLocation = sourceLocation;
        this.lastOpenedAt = lastOpenedAt;
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

    public long getLastOpenedAt() {
        return lastOpenedAt;
    }

    @Override
    public String toString() {
        return title;
    }
}
