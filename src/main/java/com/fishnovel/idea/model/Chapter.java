package com.fishnovel.idea.model;

public final class Chapter {
    private final int index;
    private final String title;
    private final String content;
    private final int startOffset;

    public Chapter(int index, String title, String content, int startOffset) {
        this.index = index;
        this.title = title;
        this.content = content;
        this.startOffset = startOffset;
    }

    public int getIndex() {
        return index;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getStartOffset() {
        return startOffset;
    }

    @Override
    public String toString() {
        return title;
    }
}
