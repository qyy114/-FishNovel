package com.fishnovel.idea.model;

public enum ReaderTheme {
    AUTO,
    PAPER,
    DARK,
    FOREST;

    public static ReaderTheme fromName(String value) {
        for (ReaderTheme theme : values()) {
            if (theme.name().equalsIgnoreCase(value)) {
                return theme;
            }
        }
        return AUTO;
    }

    @Override
    public String toString() {
        return switch (this) {
            case AUTO -> "跟随系统";
            case PAPER -> "纸张";
            case DARK -> "夜间";
            case FOREST -> "护眼";
        };
    }
}
