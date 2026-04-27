package com.fishnovel.idea.source;

import java.net.URI;
import org.junit.Assert;
import org.junit.Test;

public class RemoteChapterSourceRegistryTest {
    @Test
    public void shouldPreferSuduguAdapterBeforeGenericHtmlAdapter() {
        RemoteChapterSourceRegistry registry = RemoteChapterSourceRegistry.defaultRegistry();

        RemoteChapterSourceAdapter adapter = registry.findAdapter(URI.create("https://www.sudugu.org/5/20.html"))
            .orElseThrow();

        Assert.assertEquals("sudugu", adapter.getId());
    }

    @Test
    public void shouldUseGenericHtmlAdapterForUnknownHttpSites() {
        RemoteChapterSourceRegistry registry = RemoteChapterSourceRegistry.defaultRegistry();

        RemoteChapterSourceAdapter adapter = registry.findAdapter(URI.create("https://example.com/book/1.html"))
            .orElseThrow();

        Assert.assertEquals("generic-html", adapter.getId());
    }

    @Test
    public void shouldNotResolveChapterJumpForGenericHtmlSites() {
        RemoteChapterSourceRegistry registry = RemoteChapterSourceRegistry.defaultRegistry();

        RemoteChapterSourceAdapter adapter = registry.findAdapter(URI.create("https://example.com/book/1.html"))
            .orElseThrow();

        Assert.assertTrue(adapter.resolveChapterJump(URI.create("https://example.com/book/1.html"), 2).isEmpty());
    }

    @Test
    public void shouldPreferBqgAjaxAdapterBeforeGenericHtmlAdapter() {
        RemoteChapterSourceRegistry registry = RemoteChapterSourceRegistry.defaultRegistry();

        RemoteChapterSourceAdapter adapter = registry.findAdapter(URI.create("https://2b3ef015.bqg496.xyz/book/3588/1182.html"))
            .orElseThrow();

        Assert.assertEquals("bqg-ajax", adapter.getId());
    }

    @Test
    public void shouldRejectUnsupportedSchemes() {
        RemoteChapterSourceRegistry registry = RemoteChapterSourceRegistry.defaultRegistry();

        Assert.assertTrue(registry.findAdapter(URI.create("file:///tmp/book.html")).isEmpty());
    }
}
