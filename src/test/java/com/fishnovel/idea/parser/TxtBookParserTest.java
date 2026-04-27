package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
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
}
