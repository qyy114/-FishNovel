package com.fishnovel.idea.model;

public final class ReadingProgress {
    private final int chapterIndex;
    private final int contentOffset;
    private final long lastOpenedAt;

    public ReadingProgress(int chapterIndex, int contentOffset, long lastOpenedAt) {
        this.chapterIndex = chapterIndex;
        this.contentOffset = contentOffset;
        this.lastOpenedAt = lastOpenedAt;
    }

    public static ReadingProgress defaults() {
        return new ReadingProgress(0, 0, 0L);
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
}
