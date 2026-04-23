package com.fishnovel.idea.service;

import com.fishnovel.idea.model.ReadingPreferences;

final class ReadingPreferenceMigration {
    private ReadingPreferenceMigration() {
    }

    static void migrateLegacyPaperDefaults(ReadingStateStore.StoredState state) {
        if (state == null) {
            return;
        }
        ReadingPreferences defaults = ReadingPreferences.defaults();
        for (ReadingStateStore.StoredBookRecord book : state.books) {
            ReadingStateStore.StoredPreferences preferences = book.preferences;
            if (preferences == null) {
                continue;
            }
            boolean isLegacyDefaultFont = preferences.fontSize == 18 || preferences.fontSize == 0;
            boolean isLegacyDefaultSpacing =
                Math.abs(preferences.lineSpacing - 0.35f) < 0.0001f
                    || Math.abs(preferences.lineSpacing - defaults.getLineSpacing()) < 0.0001f;
            boolean isLegacyTheme =
                "PAPER".equalsIgnoreCase(preferences.theme)
                    || "AUTO".equalsIgnoreCase(preferences.theme)
                    || preferences.theme == null
                    || preferences.theme.isBlank();
            if (isLegacyDefaultFont && isLegacyDefaultSpacing && isLegacyTheme) {
                book.preferences = ReadingStateStore.StoredPreferences.from(defaults);
            }
        }
    }
}
