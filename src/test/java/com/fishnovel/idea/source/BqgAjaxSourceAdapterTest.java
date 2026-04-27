package com.fishnovel.idea.source;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;

public class BqgAjaxSourceAdapterTest {
    @Test
    public void shouldLoadChapterFromApiForBqgShellPage() throws Exception {
        BqgAjaxSourceAdapter adapter = new BqgAjaxSourceAdapter();
        URI uri = URI.create("https://2b3ef015.bqg496.xyz/book/3588/1182.html");

        RemoteChapterLoadResult result = adapter.loadChapter(uri, url -> {
            Assert.assertEquals("https://2b3ef015.bqg496.xyz/api/chapter?id=3588&chapterid=1182", url);
            return """
                {"id":"3588","chapterid":"1182","dirid":"3588","title":"斗罗大陆V重生唐三","author":"唐家三少","chaptername":"第1182章 法令","cs":1184,"txt":"第一行\\n第二行"}
                """.stripIndent().getBytes(StandardCharsets.UTF_8);
        });

        Assert.assertEquals("bqg-ajax", result.getSourceId());
        Assert.assertEquals("斗罗大陆V重生唐三", result.getDocument().getTitle());
        Assert.assertEquals("第1182章 法令", result.getDocument().getChapters().get(0).getTitle());
        Assert.assertEquals("第一行\n第二行", result.getDocument().getChapters().get(0).getContent());
        Assert.assertEquals("https://2b3ef015.bqg496.xyz/book/3588/1181.html", result.getNavigation().getPreviousUrl());
        Assert.assertEquals("https://2b3ef015.bqg496.xyz/book/3588/1183.html", result.getNavigation().getNextUrl());
    }

    @Test
    public void shouldSupportBqgBookChapterUrls() {
        BqgAjaxSourceAdapter adapter = new BqgAjaxSourceAdapter();

        Assert.assertTrue(adapter.supports(URI.create("https://2b3ef015.bqg496.xyz/book/3588/1182.html")));
        Assert.assertFalse(adapter.supports(URI.create("https://example.com/book/3588/1182.html")));
    }

    @Test
    public void shouldResolveChapterJumpUrlForSameBook() {
        BqgAjaxSourceAdapter adapter = new BqgAjaxSourceAdapter();

        String targetUrl = adapter.resolveChapterJump(
            URI.create("https://2b3ef015.bqg496.xyz/book/3588/1182.html"),
            1183
        ).orElseThrow();

        Assert.assertEquals("https://2b3ef015.bqg496.xyz/book/3588/1183.html", targetUrl);
    }
}
