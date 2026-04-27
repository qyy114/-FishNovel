package com.fishnovel.idea.service;

import com.fishnovel.idea.model.BookDocument;
import java.util.OptionalInt;

public final class ChapterJumpResolver {
    private ChapterJumpResolver() {
    }

    public static OptionalInt parsePositiveNumber(String input) {
        if (input == null) {
            return OptionalInt.empty();
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty() || !trimmed.matches("\\d+")) {
            return OptionalInt.empty();
        }
        try {
            int value = Integer.parseInt(trimmed);
            return value > 0 ? OptionalInt.of(value) : OptionalInt.empty();
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    public static OptionalInt resolveLocalChapterIndex(BookDocument document, String input) {
        OptionalInt chapterNumber = parsePositiveNumber(input);
        if (chapterNumber.isEmpty() || document == null) {
            return OptionalInt.empty();
        }
        int index = chapterNumber.getAsInt() - 1;
        if (index < 0 || index >= document.getChapters().size()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(index);
    }
}
