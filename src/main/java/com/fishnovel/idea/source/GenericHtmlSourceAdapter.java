package com.fishnovel.idea.source;

import com.fishnovel.idea.parser.HtmlBookParser;
import com.fishnovel.idea.parser.RemoteHtmlBookCrawler;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

public final class GenericHtmlSourceAdapter implements RemoteChapterSourceAdapter {
    private final RemoteHtmlBookCrawler crawler;

    public GenericHtmlSourceAdapter() {
        this(new RemoteHtmlBookCrawler(new HtmlBookParser()));
    }

    GenericHtmlSourceAdapter(RemoteHtmlBookCrawler crawler) {
        this.crawler = crawler;
    }

    @Override
    public String getId() {
        return "generic-html";
    }

    @Override
    public String getDisplayName() {
        return "Generic HTML";
    }

    @Override
    public boolean supports(URI uri) {
        String scheme = uri == null ? null : uri.getScheme();
        if (scheme == null) {
            return false;
        }
        String normalized = scheme.toLowerCase(Locale.ROOT);
        return "http".equals(normalized) || "https".equals(normalized);
    }

    @Override
    public RemoteChapterLoadResult loadChapter(URI uri, RemoteHtmlBookCrawler.RemotePageLoader loader) throws IOException {
        return crawler.crawlChapter(uri.toString(), loader, getId());
    }
}
