package com.fishnovel.idea.service;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Bookmark;
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

    @Test
    public void shouldRemoveBookmarkById() {
        ReadingStateStore service = new ReadingStateStore();
        BookDocument document = sampleBook("book-2", "C:/books/remove-bookmark.txt");

        service.registerBook(document);
        Bookmark bookmark = service.addBookmark(document.getBookId(), document.getTitle(), "第一章", 0, 18);

        boolean removed = service.removeBookmark(bookmark.getId());

        Assert.assertTrue(removed);
        Assert.assertTrue(service.listBookmarks(document.getBookId()).isEmpty());
    }

    @Test
    public void shouldReturnFalseWhenRemovingMissingBookmark() {
        ReadingStateStore service = new ReadingStateStore();
        BookDocument document = sampleBook("book-2b", "C:/books/missing-bookmark.txt");

        service.registerBook(document);

        boolean removed = service.removeBookmark("missing-bookmark-id");

        Assert.assertFalse(removed);
        Assert.assertTrue(service.listBookmarks(document.getBookId()).isEmpty());
    }

    @Test
    public void shouldOnlyRemoveMatchedBookmark() {
        ReadingStateStore service = new ReadingStateStore();
        BookDocument firstBook = sampleBook("book-2c", "C:/books/first.txt");
        BookDocument secondBook = sampleBook("book-2d", "C:/books/second.txt");

        service.registerBook(firstBook);
        service.registerBook(secondBook);
        Bookmark removedBookmark = service.addBookmark(firstBook.getBookId(), firstBook.getTitle(), "第一章", 0, 18);
        service.addBookmark(firstBook.getBookId(), firstBook.getTitle(), "第二章", 1, 28);
        service.addBookmark(secondBook.getBookId(), secondBook.getTitle(), "第一章", 0, 38);

        boolean removed = service.removeBookmark(removedBookmark.getId());

        Assert.assertTrue(removed);
        Assert.assertEquals(1, service.listBookmarks(firstBook.getBookId()).size());
        Assert.assertEquals(1, service.listBookmarks(secondBook.getBookId()).size());
    }

    @Test
    public void shouldMigrateLegacyPaperThemeToAuto() {
        ReadingStateStore.StoredState state = new ReadingStateStore.StoredState();
        ReadingStateStore.StoredBookRecord book = new ReadingStateStore.StoredBookRecord();
        book.bookId = "legacy-book";
        book.title = "旧配置小说";
        book.preferences = new ReadingStateStore.StoredPreferences();
        book.preferences.fontSize = 18;
        book.preferences.lineSpacing = 0.42f;
        book.preferences.theme = "PAPER";
        state.books.add(book);

        ReadingPreferenceMigration.migrateLegacyPaperDefaults(state);

        Assert.assertEquals(ReaderTheme.AUTO, book.preferences.toRuntime().getTheme());
        Assert.assertEquals(0, book.preferences.toRuntime().getFontSize());
    }

    @Test
    public void shouldRemoveBookAndAssociatedRecentEntry() {
        ReadingStateStore service = new ReadingStateStore();
        BookDocument document = sampleBook("book-3", "C:/books/remove-book.txt");

        service.registerBook(document);
        service.addBookmark(document.getBookId(), document.getTitle(), "第一章", 0, 5);
        service.removeBook(document.getBookId());

        Assert.assertTrue(service.listBooks().isEmpty());
        Assert.assertTrue(service.listRecentEntries().isEmpty());
        Assert.assertTrue(service.listBookmarks(document.getBookId()).isEmpty());
    }

    @Test
    public void shouldKeepRemoteReadingProgressAfterReopen() {
        ReadingStateStore service = new ReadingStateStore();
        BookDocument document = new BookDocument(
            "remote-book",
            "青山",
            SourceType.REMOTE_URL,
            "https://example.com/book/1.html",
            "html",
            "remote-hash",
            null,
            List.of(
                new Chapter(0, "第1章 归零", "第一页", 0),
                new Chapter(1, "第2章 出门", "第二页", 0),
                new Chapter(2, "第3章 夜路", "第三页", 0)
            )
        );

        service.registerBook(document);
        service.updateProgress(document.getBookId(), new ReadingProgress(2, 36, 999L));
        service.registerBook(document);

        Assert.assertEquals(2, service.getProgress(document.getBookId()).getChapterIndex());
        Assert.assertEquals(36, service.getProgress(document.getBookId()).getContentOffset());
        Assert.assertEquals("https://example.com/book/1.html", service.listBooks().get(0).getSourceLocation());
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
