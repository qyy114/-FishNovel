package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.source.RemoteChapterLoadResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class RemoteHtmlBookCrawlerTest {
    @Test
    public void shouldMergePaginatedPagesAndStopBeforeNextChapter() throws Exception {
        RemoteHtmlBookCrawler crawler = new RemoteHtmlBookCrawler(new HtmlBookParser());
        Set<String> loadedUrls = new LinkedHashSet<>();
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
                <a href="/book/0.html">上一章</a>
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

        RemoteChapterLoadResult result = crawler.crawlChapter(
            "https://example.com/book/1.html",
            url -> loadPage(pages, loadedUrls, url),
            "test-source"
        );
        BookDocument document = result.getDocument();

        Assert.assertEquals("青山", document.getTitle());
        Assert.assertEquals(1, document.getChapters().size());
        Assert.assertEquals("第1章 归零", document.getChapters().get(0).getTitle());
        Assert.assertTrue(document.getChapters().get(0).getContent().contains("第一页正文"));
        Assert.assertTrue(document.getChapters().get(0).getContent().contains("第二页正文"));
        Assert.assertFalse(loadedUrls.contains("https://example.com/book/2.html"));
        Assert.assertEquals("https://example.com/book/0.html", result.getNavigation().getPreviousUrl());
        Assert.assertEquals("https://example.com/book/2.html", result.getNavigation().getNextUrl());
        Assert.assertEquals("test-source", result.getSourceId());
    }

    @Test
    public void shouldKeepStableBookIdAcrossDifferentChapterUrls() throws Exception {
        RemoteHtmlBookCrawler crawler = new RemoteHtmlBookCrawler(new HtmlBookParser());
        Map<String, byte[]> pages = Map.of(
            "https://example.com/book/1.html", page("青山 > 第1章 归零", "第一页正文"),
            "https://example.com/book/2.html", page("青山 > 第2章 出门", "第二章正文")
        );

        BookDocument first = crawler.crawl("https://example.com/book/1.html", url -> loadPage(pages, new LinkedHashSet<>(), url));
        BookDocument second = crawler.crawl("https://example.com/book/2.html", url -> loadPage(pages, new LinkedHashSet<>(), url));

        Assert.assertEquals(first.getBookId(), second.getBookId());
        Assert.assertEquals("https://example.com/book/1.html", first.getSourceLocation());
        Assert.assertEquals("https://example.com/book/2.html", second.getSourceLocation());
    }

    private byte[] page(String title, String content) {
        return ("""
            <html>
            <body>
            <h1>%s</h1>
            <div class="con"><p>%s</p></div>
            </body>
            </html>
            """.formatted(title, content).stripIndent()).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] loadPage(Map<String, byte[]> pages, Set<String> loadedUrls, String url) throws IOException {
        loadedUrls.add(url);
        byte[] page = pages.get(url);
        if (page == null) {
            throw new IOException("Missing page: " + url);
        }
        return page;
    }
}
