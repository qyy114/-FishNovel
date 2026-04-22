package com.fishnovel.idea.model;

public enum ReaderTheme {
    PAPER,
    DARK,
    FOREST;

    public static ReaderTheme fromName(String value) {
        for (ReaderTheme theme : values()) {
            if (theme.name().equalsIgnoreCase(value)) {
                return theme;
            }
        }
        return PAPER;
    }
}
