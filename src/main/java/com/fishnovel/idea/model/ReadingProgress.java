package com.fishnovel.idea.model;

public final class ReadingProgress {
    private final int chapterIndex;
    private final int contentOffset;
    private final long lastOpenedAt;
    private final String chapterKey;

    public ReadingProgress(int chapterIndex, int contentOffset, long lastOpenedAt) {
        this(chapterIndex, contentOffset, lastOpenedAt, null);
    }

    public ReadingProgress(int chapterIndex, int contentOffset, long lastOpenedAt, String chapterKey) {
        this.chapterIndex = chapterIndex;
        this.contentOffset = contentOffset;
        this.lastOpenedAt = lastOpenedAt;
        this.chapterKey = chapterKey;
    }

    public static ReadingProgress defaults() {
        return new ReadingProgress(0, 0, 0L, null);
    }

    public int getChapterIndex() {
        return chapterIndex;
    }

    public int getContentOffset() {
        return contentOffset;
    }

    public long getLastOpenedAt() {
        return lastOpenedAt;
    }

    public String getChapterKey() {
        return chapterKey;
    }
}
