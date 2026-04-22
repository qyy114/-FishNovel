package com.fishnovel.idea.service;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.ReaderTheme;
import com.fishnovel.idea.model.ReadingPreferences;
import com.fishnovel.idea.model.ReadingProgress;
import com.fishnovel.idea.model.SourceType;
import java.nio.file.Path;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ReadingStateServiceTest {
    @Test
    public void shouldRememberProgressPreferencesAndBookmarks() {
        ReadingStateStore service = new ReadingStateStore();
        BookDocument document = sampleBook("book-1", "C:/books/novel.txt");

        service.registerBook(document);
        service.updateProgress(document.getBookId(), new ReadingProgress(1, 128, 123456L));
        service.updatePreferences(document.getBookId(), new ReadingPreferences(20, 0.4f, ReaderTheme.DARK));
        service.addBookmark(document.getBookId(), document.getTitle(), "第二章", 1, 128);

        Assert.assertEquals(1, service.listBooks().size());
        Assert.assertEquals(1, service.listRecentEntries().size());
        Assert.assertEquals(1, service.listBookmarks(document.getBookId()).size());
        Assert.assertEquals(1, service.getProgress(document.getBookId()).getChapterIndex());
        Assert.assertEquals(128, service.getProgress(document.getBookId()).getContentOffset());
        Assert.assertEquals(20, service.getPreferences(document.getBookId()).getFontSize());
        Assert.assertEquals(ReaderTheme.DARK, service.getPreferences(document.getBookId()).getTheme());
    }

    @Test
    public void shouldMergeSameBookWhenPathChanges() {
        ReadingStateStore service = new ReadingStateStore();
        BookDocument original = sampleBook("same-book", "C:/books/original.txt");
        BookDocument renamed = sampleBook("same-book", "D:/archive/renamed.txt");

        service.registerBook(original);
        service.registerBook(renamed);

        Assert.assertEquals(1, service.listBooks().size());
        Assert.assertEquals("D:/archive/renamed.txt", service.listBooks().get(0).getSourceLocation());
    }

    private BookDocument sampleBook(String bookId, String path) {
        return new BookDocument(
            bookId,
            "测试小说",
            SourceType.LOCAL_FILE,
            path,
            "txt",
            "hash-value",
            Path.of(path),
            List.of(
                new Chapter(0, "第一章", "第一章内容", 0),
                new Chapter(1, "第二章", "第二章内容", 50)
            )
        );
    }
}
