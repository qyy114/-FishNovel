package com.fishnovel.idea.model;

public final class Bookmark {
    private final String id;
    private final String bookId;
    private final String bookTitle;
    private final String chapterTitle;
    private final int chapterIndex;
    private final int contentOffset;
    private final long createdAt;

    public Bookmark(String id, String bookId, String bookTitle, String chapterTitle, int chapterIndex, int contentOffset, long createdAt) {
        this.id = id;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.chapterTitle = chapterTitle;
        this.chapterIndex = chapterIndex;
        this.contentOffset = contentOffset;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public int getChapterIndex() {
        return chapterIndex;
    }

    public int getContentOffset() {
        return contentOffset;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return bookTitle + " · " + chapterTitle;
    }
}
