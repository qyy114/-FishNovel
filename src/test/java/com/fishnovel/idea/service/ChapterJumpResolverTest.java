package com.fishnovel.idea.service;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.SourceType;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ChapterJumpResolverTest {
    @Test
    public void shouldResolveOneBasedLocalChapterNumber() {
        BookDocument document = new BookDocument(
            "book",
            "Book",
            SourceType.LOCAL_FILE,
            "book.txt",
            "txt",
            "hash",
            null,
            List.of(
                new Chapter(0, "Chapter 1", "one", 0),
                new Chapter(1, "Chapter 2", "two", 10)
            )
        );

        Assert.assertEquals(0, ChapterJumpResolver.resolveLocalChapterIndex(document, "1").orElseThrow());
        Assert.assertEquals(1, ChapterJumpResolver.resolveLocalChapterIndex(document, "002").orElseThrow());
    }

    @Test
    public void shouldRejectInvalidOrOutOfRangeChapterNumber() {
        BookDocument document = new BookDocument(
            "book",
            "Book",
            SourceType.LOCAL_FILE,
            "book.txt",
            "txt",
            "hash",
            null,
            List.of(new Chapter(0, "Chapter 1", "one", 0))
        );

        Assert.assertTrue(ChapterJumpResolver.parsePositiveNumber("abc").isEmpty());
        Assert.assertTrue(ChapterJumpResolver.parsePositiveNumber("0").isEmpty());
        Assert.assertTrue(ChapterJumpResolver.resolveLocalChapterIndex(document, "2").isEmpty());
    }
}
