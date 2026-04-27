package com.fishnovel.idea.source;

import com.fishnovel.idea.parser.HtmlBookParser;
import com.fishnovel.idea.parser.RemoteHtmlBookCrawler;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

public final class SuduguSourceAdapter implements RemoteChapterSourceAdapter {
    private final RemoteHtmlBookCrawler crawler;

    public SuduguSourceAdapter() {
        this(new RemoteHtmlBookCrawler(new HtmlBookParser()));
    }

    SuduguSourceAdapter(RemoteHtmlBookCrawler crawler) {
        this.crawler = crawler;
    }

    @Override
    public String getId() {
        return "sudugu";
    }

    @Override
    public String getDisplayName() {
        return "Sudugu";
    }

    @Override
    public boolean supports(URI uri) {
        String host = uri == null ? null : uri.getHost();
        if (host == null) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return "sudugu.org".equals(normalizedHost) || normalizedHost.endsWith(".sudugu.org");
    }

    @Override
    public RemoteChapterLoadResult loadChapter(URI uri, RemoteHtmlBookCrawler.RemotePageLoader loader) throws IOException {
        return crawler.crawlChapter(uri.toString(), loader, getId());
    }
}
