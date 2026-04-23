package com.fishnovel.idea.model;

public final class ReadingPreferences {
    private final int fontSize;
    private final float lineSpacing;
    private final ReaderTheme theme;

    public ReadingPreferences(int fontSize, float lineSpacing, ReaderTheme theme) {
        this.fontSize = fontSize;
        this.lineSpacing = lineSpacing;
        this.theme = theme;
    }

    public static ReadingPreferences defaults() {
        return new ReadingPreferences(0, 0.42f, ReaderTheme.AUTO);
    }

    public int getFontSize() {
        return fontSize;
    }

    public float getLineSpacing() {
        return lineSpacing;
    }

    public ReaderTheme getTheme() {
        return theme;
    }
}
