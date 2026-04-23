package com.fishnovel.idea.service;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.BookShelfItem;
import com.fishnovel.idea.model.Bookmark;
import com.fishnovel.idea.model.ReaderTheme;
import com.fishnovel.idea.model.ReadingPreferences;
import com.fishnovel.idea.model.ReadingProgress;
import com.fishnovel.idea.model.RecentEntry;
import com.fishnovel.idea.model.SourceType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ReadingStateStore {
    private StoredState state = new StoredState();

    public StoredState getState() {
        return state;
    }

    public void loadState(StoredState state) {
        this.state = state == null ? new StoredState() : state;
    }

    public synchronized void registerBook(BookDocument document) {
        StoredBookRecord record = findOrCreateRecord(document.getBookId());
        record.bookId = document.getBookId();
        record.title = document.getTitle();
        record.sourceLocation = document.getSourceLocation();
        record.sourceType = document.getSourceType().name();
        record.contentHash = document.getContentHash();
        if (record.preferences == null) {
            record.preferences = StoredPreferences.from(ReadingPreferences.defaults());
        }
        if (record.progress == null) {
            record.progress = StoredProgress.from(ReadingProgress.defaults());
        }
        touchRecent(document.getBookId(), document.getTitle(), document.getSourceLocation(), System.currentTimeMillis());
    }

    public synchronized List<BookShelfItem> listBooks() {
        return state.books.stream()
            .map(record -> new BookShelfItem(
                record.bookId,
                record.title,
                record.sourceLocation,
                SourceType.valueOf(record.sourceType == null ? SourceType.LOCAL_FILE.name() : record.sourceType),
                record.progress == null ? 0L : record.progress.lastOpenedAt,
                record.bookmarks == null ? 0 : record.bookmarks.size()
            ))
            .sorted(Comparator.comparingLong(BookShelfItem::getLastOpenedAt).reversed().thenComparing(BookShelfItem::getTitle))
            .toList();
    }

    public synchronized Optional<BookShelfItem> findBook(String bookId) {
        return listBooks().stream().filter(item -> item.getBookId().equals(bookId)).findFirst();
    }

    public synchronized List<RecentEntry> listRecentEntries() {
        return state.recentEntries.stream()
            .map(entry -> new RecentEntry(entry.bookId, entry.title, entry.sourceLocation, entry.lastOpenedAt))
            .sorted(Comparator.comparingLong(RecentEntry::getLastOpenedAt).reversed())
            .toList();
    }

    public synchronized List<Bookmark> listBookmarks() {
        List<Bookmark> bookmarks = new ArrayList<>();
        for (StoredBookRecord book : state.books) {
            if (book.bookmarks == null) {
                continue;
            }
            for (StoredBookmark bookmark : book.bookmarks) {
                bookmarks.add(bookmark.toRuntime());
            }
        }
        bookmarks.sort(Comparator.comparingLong(Bookmark::getCreatedAt).reversed());
        return bookmarks;
    }

    public synchronized List<Bookmark> listBookmarks(String bookId) {
        return findRecord(bookId)
            .map(record -> record.bookmarks == null
                ? List.<Bookmark>of()
                : record.bookmarks.stream()
                    .map(StoredBookmark::toRuntime)
                    .sorted(Comparator.comparingLong(Bookmark::getCreatedAt).reversed())
                    .toList())
            .orElseGet(List::of);
    }

    public synchronized ReadingProgress getProgress(String bookId) {
        return findRecord(bookId)
            .map(record -> record.progress == null ? ReadingProgress.defaults() : record.progress.toRuntime())
            .orElseGet(ReadingProgress::defaults);
    }

    public synchronized ReadingPreferences getPreferences(String bookId) {
        return findRecord(bookId)
            .map(record -> record.preferences == null ? ReadingPreferences.defaults() : record.preferences.toRuntime())
            .orElseGet(ReadingPreferences::defaults);
    }

    public synchronized void updateProgress(String bookId, ReadingProgress progress) {
        StoredBookRecord record = findOrCreateRecord(bookId);
        record.progress = StoredProgress.from(progress);
        touchRecent(bookId, record.title, record.sourceLocation, progress.getLastOpenedAt());
    }

    public synchronized void updatePreferences(String bookId, ReadingPreferences preferences) {
        StoredBookRecord record = findOrCreateRecord(bookId);
        record.preferences = StoredPreferences.from(preferences);
    }

    public synchronized Bookmark addBookmark(String bookId, String bookTitle, String chapterTitle, int chapterIndex, int contentOffset) {
        StoredBookRecord record = findOrCreateRecord(bookId);
        if (record.bookmarks == null) {
            record.bookmarks = new ArrayList<>();
        }
        StoredBookmark storedBookmark = new StoredBookmark();
        storedBookmark.id = UUID.randomUUID().toString();
        storedBookmark.bookId = bookId;
        storedBookmark.bookTitle = bookTitle;
        storedBookmark.chapterTitle = chapterTitle;
        storedBookmark.chapterIndex = chapterIndex;
        storedBookmark.contentOffset = contentOffset;
        storedBookmark.createdAt = System.currentTimeMillis();
        record.bookmarks.add(storedBookmark);
        return storedBookmark.toRuntime();
    }

    public synchronized boolean removeBookmark(String bookmarkId) {
        boolean removed = false;
        for (StoredBookRecord book : state.books) {
            if (book.bookmarks == null) {
                continue;
            }
            removed |= book.bookmarks.removeIf(bookmark -> bookmarkId.equals(bookmark.id));
        }
        return removed;
    }

    public synchronized void removeBook(String bookId) {
        state.books.removeIf(book -> bookId.equals(book.bookId));
        state.recentEntries.removeIf(entry -> bookId.equals(entry.bookId));
    }

    private Optional<StoredBookRecord> findRecord(String bookId) {
        return state.books.stream().filter(record -> bookId.equals(record.bookId)).findFirst();
    }

    private StoredBookRecord findOrCreateRecord(String bookId) {
        return findRecord(bookId).orElseGet(() -> {
            StoredBookRecord record = new StoredBookRecord();
            record.bookId = bookId;
            record.bookmarks = new ArrayList<>();
            state.books.add(record);
            return record;
        });
    }

    private void touchRecent(String bookId, String title, String sourceLocation, long lastOpenedAt) {
        state.recentEntries.removeIf(entry -> bookId.equals(entry.bookId));
        StoredRecentEntry entry = new StoredRecentEntry();
        entry.bookId = bookId;
        entry.title = title == null ? "未命名小说" : title;
        entry.sourceLocation = sourceLocation == null ? "" : sourceLocation;
        entry.lastOpenedAt = lastOpenedAt;
        state.recentEntries.add(entry);
        state.recentEntries.sort(Comparator.comparingLong(item -> -item.lastOpenedAt));
        if (state.recentEntries.size() > 20) {
            state.recentEntries = new ArrayList<>(state.recentEntries.subList(0, 20));
        }
    }

    public static final class StoredState {
        public List<StoredBookRecord> books = new ArrayList<>();
        public List<StoredRecentEntry> recentEntries = new ArrayList<>();
    }

    public static final class StoredBookRecord {
        public String bookId;
        public String title;
        public String sourceLocation;
        public String sourceType;
        public String contentHash;
        public StoredProgress progress;
        public StoredPreferences preferences;
        public List<StoredBookmark> bookmarks = new ArrayList<>();
    }

    public static final class StoredRecentEntry {
        public String bookId;
        public String title;
        public String sourceLocation;
        public long lastOpenedAt;
    }

    public static final class StoredProgress {
        public int chapterIndex;
        public int contentOffset;
        public long lastOpenedAt;
        public String chapterKey;

        public ReadingProgress toRuntime() {
            return new ReadingProgress(chapterIndex, contentOffset, lastOpenedAt, chapterKey);
        }

        public static StoredProgress from(ReadingProgress progress) {
            StoredProgress storedProgress = new StoredProgress();
            storedProgress.chapterIndex = progress.getChapterIndex();
            storedProgress.contentOffset = progress.getContentOffset();
            storedProgress.lastOpenedAt = progress.getLastOpenedAt();
            storedProgress.chapterKey = progress.getChapterKey();
            return storedProgress;
        }
    }

    public static final class StoredPreferences {
        public int fontSize;
        public float lineSpacing;
        public String theme;

        public ReadingPreferences toRuntime() {
            return new ReadingPreferences(fontSize, lineSpacing, ReaderTheme.fromName(theme));
        }

        public static StoredPreferences from(ReadingPreferences preferences) {
            StoredPreferences storedPreferences = new StoredPreferences();
            storedPreferences.fontSize = preferences.getFontSize();
            storedPreferences.lineSpacing = preferences.getLineSpacing();
            storedPreferences.theme = preferences.getTheme().name();
            return storedPreferences;
        }
    }

    public static final class StoredBookmark {
        public String id;
        public String bookId;
        public String bookTitle;
        public String chapterTitle;
        public int chapterIndex;
        public int contentOffset;
        public long createdAt;

        public Bookmark toRuntime() {
            return new Bookmark(id, bookId, bookTitle, chapterTitle, chapterIndex, contentOffset, createdAt);
        }
    }
}
