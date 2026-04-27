package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.source.TomatoSourceLocation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;

public class TxtBookParserTest {
    @Test
    public void shouldSplitCommonChineseChapterHeadings() throws Exception {
        Path file = Files.createTempFile("fishnovel-txt-chapters", ".txt");
        Files.writeString(file, """
            \u7b2c\u4e00\u7ae0 \u521d\u89c1
            one
            \u7b2c1\u8bdd \u518d\u89c1
            two
            \u7b2c001\u56de \u5f52\u6765
            three
            \u5377\u4e00 \u5f00\u7bc7
            four
            """.stripIndent(), StandardCharsets.UTF_8);

        BookDocument document = new TxtBookParser().parse(file);

        Assert.assertEquals(4, document.getChapters().size());
        Assert.assertEquals("\u7b2c\u4e00\u7ae0 \u521d\u89c1", document.getChapters().get(0).getTitle());
        Assert.assertEquals("\u7b2c1\u8bdd \u518d\u89c1", document.getChapters().get(1).getTitle());
        Assert.assertEquals("\u7b2c001\u56de \u5f52\u6765", document.getChapters().get(2).getTitle());
        Assert.assertEquals("\u5377\u4e00 \u5f00\u7bc7", document.getChapters().get(3).getTitle());
    }

    @Test
    public void shouldParseTomatoTxtWithStableBookIdentity() throws Exception {
        Path firstFile = Files.createTempFile("fishnovel-tomato-first", ".txt");
        Path secondFile = Files.createTempFile("fishnovel-tomato-second", ".txt");
        Files.writeString(firstFile, "\u7b2c\u4e00\u7ae0 \u521d\u89c1\none", StandardCharsets.UTF_8);
        Files.writeString(secondFile, "\u7b2c\u4e00\u7ae0 \u521d\u89c1\nupdated", StandardCharsets.UTF_8);

        TxtBookParser parser = new TxtBookParser();
        BookDocument first = parser.parseTomato(firstFile, "7423591956359416856", "\u6d4b\u8bd5\u4e66");
        BookDocument second = parser.parseTomato(secondFile, "7423591956359416856", "\u6d4b\u8bd5\u4e66");

        Assert.assertEquals(SourceType.TOMATO_TXT, first.getSourceType());
        Assert.assertEquals(TomatoSourceLocation.toLocation("7423591956359416856"), first.getSourceLocation());
        Assert.assertEquals(first.getBookId(), second.getBookId());
        Assert.assertNotEquals(first.getContentHash(), second.getContentHash());
    }
}
