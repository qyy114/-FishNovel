package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class RemoteHtmlBookCrawlerTest {
    @Test
    public void shouldMergePaginatedPagesIntoSingleChapterAndFollowNextChapter() throws Exception {
        HtmlBookParser parser = new HtmlBookParser();
        RemoteHtmlBookCrawler crawler = new RemoteHtmlBookCrawler(parser);

        Map<String, byte[]> pages = Map.of(
            "https://example.com/book/1.html", """
                <html>
                <body>
                <h1>青山 &gt; 第1章 归零</h1>
                <div class="con"><p>第一页正文</p></div>
                <a href="/book/1-2.html">下一页</a>
                </body>
                </html>
                """.stripIndent().getBytes(StandardCharsets.UTF_8),
            "https://example.com/book/1-2.html", """
                <html>
                <body>
                <h1>青山 &gt; 第1章 归零</h1>
                <div class="con"><p>第二页正文</p></div>
                <a href="/book/2.html">下一章</a>
                </body>
                </html>
                """.stripIndent().getBytes(StandardCharsets.UTF_8),
            "https://example.com/book/2.html", """
                <html>
                <body>
                <h1>青山 &gt; 第2章 出门</h1>
                <div class="con"><p>第二章正文</p></div>
                </body>
                </html>
                """.stripIndent().getBytes(StandardCharsets.UTF_8)
        );

        BookDocument document = crawler.crawl("https://example.com/book/1.html", url -> loadPage(pages, url));

        Assert.assertEquals("青山", document.getTitle());
        Assert.assertEquals(2, document.getChapters().size());
        Assert.assertEquals("第1章 归零", document.getChapters().get(0).getTitle());
        Assert.assertTrue(document.getChapters().get(0).getContent().contains("第一页正文"));
        Assert.assertTrue(document.getChapters().get(0).getContent().contains("第二页正文"));
        Assert.assertEquals("第2章 出门", document.getChapters().get(1).getTitle());
        Assert.assertTrue(document.getChapters().get(1).getContent().contains("第二章正文"));
    }

    private byte[] loadPage(Map<String, byte[]> pages, String url) throws IOException {
        byte[] page = pages.get(url);
        if (page == null) {
            throw new IOException("Missing page: " + url);
        }
        return page;
    }
}
