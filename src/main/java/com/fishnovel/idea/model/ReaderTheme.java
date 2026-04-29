package com.fishnovel.idea.model;

import com.fishnovel.idea.FishNovelBundle;

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
            case AUTO -> FishNovelBundle.message("reader.theme.auto");
            case PAPER -> FishNovelBundle.message("reader.theme.paper");
            case DARK -> FishNovelBundle.message("reader.theme.dark");
            case FOREST -> FishNovelBundle.message("reader.theme.forest");
        };
    }
}
