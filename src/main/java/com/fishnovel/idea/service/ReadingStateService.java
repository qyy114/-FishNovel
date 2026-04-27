package com.fishnovel.idea.service;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.BookShelfItem;
import com.fishnovel.idea.model.Bookmark;
import com.fishnovel.idea.model.ReadingPreferences;
import com.fishnovel.idea.model.ReadingProgress;
import com.fishnovel.idea.model.RecentEntry;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import java.util.List;
import java.util.Optional;

@State(name = "FishNovelReadingState", storages = @Storage("fishNovel.xml"))
public final class ReadingStateService implements PersistentStateComponent<ReadingStateStore.StoredState> {
    private final ReadingStateStore store = new ReadingStateStore();

    public static ReadingStateService getInstance() {
        return ApplicationManager.getApplication().getService(ReadingStateService.class);
    }

    @Override
    public ReadingStateStore.StoredState getState() {
        return store.getState();
    }

    @Override
    public void loadState(ReadingStateStore.StoredState state) {
        store.loadState(state);
        ReadingPreferenceMigration.migrateLegacyPaperDefaults(store.getState());
    }

    public void registerBook(BookDocument document) {
        store.registerBook(document);
    }

    public List<BookShelfItem> listBooks() {
        return store.listBooks();
    }

    public Optional<BookShelfItem> findBook(String bookId) {
        return store.findBook(bookId);
    }

    public List<RecentEntry> listRecentEntries() {
        return store.listRecentEntries();
    }

    public List<Bookmark> listBookmarks() {
        return store.listBookmarks();
    }

    public List<Bookmark> listBookmarks(String bookId) {
        return store.listBookmarks(bookId);
    }

    public ReadingProgress getProgress(String bookId) {
        return store.getProgress(bookId);
    }

    public ReadingPreferences getPreferences(String bookId) {
        return store.getPreferences(bookId);
    }

    public void updateProgress(String bookId, ReadingProgress progress) {
        store.updateProgress(bookId, progress);
    }

    public void updatePreferences(String bookId, ReadingPreferences preferences) {
        store.updatePreferences(bookId, preferences);
    }

    public Bookmark addBookmark(String bookId, String bookTitle, String chapterTitle, int chapterIndex, int contentOffset) {
        return store.addBookmark(bookId, bookTitle, chapterTitle, chapterIndex, contentOffset);
    }

    public boolean removeBookmark(String bookmarkId) {
        return store.removeBookmark(bookmarkId);
    }

    public void removeBook(String bookId) {
        store.removeBook(bookId);
    }

    public Optional<String> getTomatoDownloaderPath() {
        return store.getTomatoDownloaderPath();
    }

    public void setTomatoDownloaderPath(String tomatoDownloaderPath) {
        store.setTomatoDownloaderPath(tomatoDownloaderPath);
    }
}
