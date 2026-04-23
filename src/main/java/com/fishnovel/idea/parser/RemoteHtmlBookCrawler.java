package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.util.BookIdGenerator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RemoteHtmlBookCrawler {
    private static final int MAX_CHAPTERS = 120;
    private static final int MAX_PAGES = 240;

    private final HtmlBookParser parser;

    public RemoteHtmlBookCrawler(HtmlBookParser parser) {
        this.parser = parser;
    }

    public BookDocument crawl(String startUrl, RemotePageLoader loader) throws IOException {
        Set<String> visitedUrls = new LinkedHashSet<>();
        List<Chapter> chapters = new ArrayList<>();
        String currentUrl = startUrl;
        String bookTitle = "";
        String currentChapterTitle = null;
        StringBuilder currentChapterContent = new StringBuilder();
        int pageCount = 0;

        while (currentUrl != null
            && !currentUrl.isBlank()
            && visitedUrls.add(currentUrl)
            && pageCount < MAX_PAGES
            && chapters.size() < MAX_CHAPTERS) {
            RemoteHtmlPage page = parser.parseRemotePage(currentUrl, loader.load(currentUrl));
            pageCount++;

            if (bookTitle.isBlank()) {
                bookTitle = page.getBookTitle();
            }
            if (currentChapterTitle == null) {
                currentChapterTitle = page.getChapterTitle();
            }
            if (!Objects.equals(currentChapterTitle, page.getChapterTitle())) {
                chapters.add(new Chapter(chapters.size(), currentChapterTitle, currentChapterContent.toString().trim(), 0));
                currentChapterTitle = page.getChapterTitle();
                currentChapterContent = new StringBuilder();
                if (chapters.size() >= MAX_CHAPTERS) {
                    break;
                }
            }

            appendChapterPage(currentChapterContent, page.getContent());
            currentUrl = sanitizeNextUrl(page.getNextUrl(), visitedUrls);
        }

        if (currentChapterTitle != null && currentChapterContent.length() > 0 && chapters.size() < MAX_CHAPTERS) {
            chapters.add(new Chapter(chapters.size(), currentChapterTitle, currentChapterContent.toString().trim(), 0));
        }

        if (chapters.isEmpty()) {
            throw new IOException("网页正文解析失败，未找到可阅读的章节内容。");
        }

        String resolvedTitle = bookTitle.isBlank() ? chapters.get(0).getTitle() : bookTitle;
        String digestSource = startUrl + "#" + chapters.size() + "#" + visitedUrls.size();
        return new BookDocument(
            BookIdGenerator.fromBytes(startUrl.getBytes(StandardCharsets.UTF_8)),
            resolvedTitle,
            SourceType.REMOTE_URL,
            startUrl,
            "html",
            BookIdGenerator.fromBytes(digestSource.getBytes(StandardCharsets.UTF_8)),
            null,
            chapters
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

    @FunctionalInterface
    public interface RemotePageLoader {
        byte[] load(String url) throws IOException;
    }
}
