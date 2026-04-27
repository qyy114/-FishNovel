package com.fishnovel.idea.service;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.ReadingProgress;
import com.fishnovel.idea.model.SourceType;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ReadingProgressResolverTest {
    @Test
    public void shouldPreferChapterKeyWhenResolvingProgress() {
        BookDocument document = new BookDocument(
            "remote-book",
            "青山",
            SourceType.REMOTE_URL,
            "https://example.com/book/1.html",
            "html",
            "hash",
            null,
            List.of(
                new Chapter(0, "序章", "序章内容", 0),
                new Chapter(1, "第1章 归零", "第一章内容", 0),
                new Chapter(2, "第2章 出门", "第二章内容", 0)
            )
        );

        ReadingProgress progress = new ReadingProgress(0, 18, 1000L, "第2章 出门");

        Assert.assertEquals(2, ReadingProgressResolver.resolveChapterIndex(document, progress));
    }

    @Test
    public void shouldFallbackToChapterIndexWhenChapterKeyMissing() {
        BookDocument document = new BookDocument(
            "remote-book",
            "青山",
            SourceType.REMOTE_URL,
            "https://example.com/book/1.html",
            "html",
            "hash",
            null,
            List.of(
                new Chapter(0, "序章", "序章内容", 0),
                new Chapter(1, "第1章 归零", "第一章内容", 0)
            )
        );

        ReadingProgress progress = new ReadingProgress(1, 18, 1000L, "不存在的章节");

        Assert.assertEquals(1, ReadingProgressResolver.resolveChapterIndex(document, progress));
    }

    @Test
    public void shouldResetOffsetWhenStoredChapterKeyDoesNotMatchResolvedChapter() {
        BookDocument document = new BookDocument(
            "remote-book",
            "Green Hill",
            SourceType.REMOTE_URL,
            "https://example.com/book/2.html",
            "html",
            "hash",
            null,
            List.of(new Chapter(0, "Chapter 2", "content", 0))
        );
        ReadingProgress progress = new ReadingProgress(0, 88, 1000L, "Chapter 1");

        Assert.assertEquals(0, ReadingProgressResolver.resolveContentOffset(document, progress, 0));
    }

    @Test
    public void shouldKeepOffsetWhenStoredChapterKeyMatchesResolvedChapter() {
        BookDocument document = new BookDocument(
            "remote-book",
            "Green Hill",
            SourceType.REMOTE_URL,
            "https://example.com/book/2.html",
            "html",
            "hash",
            null,
            List.of(new Chapter(0, "Chapter 2", "content", 0))
        );
        ReadingProgress progress = new ReadingProgress(0, 88, 1000L, "Chapter 2");

        Assert.assertEquals(88, ReadingProgressResolver.resolveContentOffset(document, progress, 0));
    }
}
