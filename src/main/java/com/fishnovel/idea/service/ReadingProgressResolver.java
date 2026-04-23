package com.fishnovel.idea.service;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.ReadingProgress;

public final class ReadingProgressResolver {
    private ReadingProgressResolver() {
    }

    public static int resolveChapterIndex(BookDocument document, ReadingProgress progress) {
        if (document.getChapters().isEmpty()) {
            return 0;
        }
        String chapterKey = progress.getChapterKey();
        if (chapterKey != null && !chapterKey.isBlank()) {
            for (Chapter chapter : document.getChapters()) {
                if (chapterKey.equals(chapter.getTitle())) {
                    return chapter.getIndex();
                }
            }
        }
        return Math.max(0, Math.min(progress.getChapterIndex(), document.getChapters().size() - 1));
    }
}
