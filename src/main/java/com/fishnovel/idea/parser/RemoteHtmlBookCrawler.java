package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.source.RemoteChapterLoadResult;
import com.fishnovel.idea.source.RemoteChapterNavigation;
import com.fishnovel.idea.util.BookIdGenerator;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RemoteHtmlBookCrawler {
    private static final int MAX_PAGES_PER_CHAPTER = 24;

    private final HtmlBookParser parser;

    public RemoteHtmlBookCrawler(HtmlBookParser parser) {
        this.parser = parser;
    }

    public BookDocument crawl(String startUrl, RemotePageLoader loader) throws IOException {
        return crawlChapter(startUrl, loader, "generic-html").getDocument();
    }

    public RemoteChapterLoadResult crawlChapter(String startUrl, RemotePageLoader loader, String sourceId) throws IOException {
        Set<String> visitedUrls = new LinkedHashSet<>();
        String currentUrl = startUrl;
        String bookTitle = "";
        String chapterTitle = null;
        String previousChapterUrl = null;
        String nextChapterUrl = null;
        StringBuilder chapterContent = new StringBuilder();
        int pageCount = 0;

        while (currentUrl != null
            && !currentUrl.isBlank()
            && visitedUrls.add(currentUrl)
            && pageCount < MAX_PAGES_PER_CHAPTER) {
            RemoteHtmlPage page = parser.parseRemotePage(currentUrl, loader.load(currentUrl));
            pageCount++;

            if (bookTitle.isBlank()) {
                bookTitle = page.getBookTitle();
            }
            if (chapterTitle == null || chapterTitle.isBlank()) {
                chapterTitle = page.getChapterTitle();
            }
            if (previousChapterUrl == null) {
                previousChapterUrl = page.getPreviousChapterUrl();
            }

            appendChapterPage(chapterContent, page.getContent());
            String nextPageUrl = sanitizeNextUrl(page.getNextPageUrl(), visitedUrls);
            if (nextPageUrl == null) {
                nextChapterUrl = page.getNextChapterUrl();
                break;
            }
            currentUrl = nextPageUrl;
        }

        if (chapterTitle == null || chapterTitle.isBlank() || chapterContent.length() == 0) {
            throw new IOException("No readable chapter content was found on the web page.");
        }

        String resolvedTitle = bookTitle.isBlank() ? chapterTitle : bookTitle;
        BookDocument document = new BookDocument(
            BookIdGenerator.fromBytes(bookIdentity(startUrl, resolvedTitle).getBytes(StandardCharsets.UTF_8)),
            resolvedTitle,
            SourceType.REMOTE_URL,
            startUrl,
            "html",
            BookIdGenerator.fromBytes((startUrl + "#" + visitedUrls.size()).getBytes(StandardCharsets.UTF_8)),
            null,
            List.of(new Chapter(0, chapterTitle, chapterContent.toString().trim(), 0))
        );
        String warning = pageCount >= MAX_PAGES_PER_CHAPTER
            ? "Stopped after loading the maximum number of pages for one chapter."
            : null;
        return new RemoteChapterLoadResult(
            document,
            new RemoteChapterNavigation(startUrl, previousChapterUrl, nextChapterUrl),
            sourceId,
            warning
        );
    }

    private void appendChapterPage(StringBuilder builder, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append(content.trim());
    }

    private String sanitizeNextUrl(String nextUrl, Set<String> visitedUrls) {
        if (nextUrl == null || nextUrl.isBlank() || visitedUrls.contains(nextUrl)) {
            return null;
        }
        return nextUrl;
    }

    private String bookIdentity(String startUrl, String title) {
        URI uri = URI.create(startUrl);
        String host = uri.getHost() == null ? "" : uri.getHost();
        String path = uri.getPath() == null ? "" : uri.getPath();
        int lastSlash = path.lastIndexOf('/');
        String directory = lastSlash <= 0 ? path : path.substring(0, lastSlash);
        return host + directory + "#" + title;
    }

    @FunctionalInterface
    public interface RemotePageLoader {
        byte[] load(String url) throws IOException;
    }
}
