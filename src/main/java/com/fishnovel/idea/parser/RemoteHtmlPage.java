package com.fishnovel.idea.parser;

public final class RemoteHtmlPage {
    private final String bookTitle;
    private final String chapterTitle;
    private final String content;
    private final String previousChapterUrl;
    private final String nextPageUrl;
    private final String nextChapterUrl;
    private final String nextUrl;

    public RemoteHtmlPage(String bookTitle, String chapterTitle, String content, String nextUrl) {
        this(bookTitle, chapterTitle, content, null, null, nextUrl);
    }

    public RemoteHtmlPage(
        String bookTitle,
        String chapterTitle,
        String content,
        String previousChapterUrl,
        String nextPageUrl,
        String nextChapterUrl
    ) {
        this.bookTitle = bookTitle;
        this.chapterTitle = chapterTitle;
        this.content = content;
        this.previousChapterUrl = previousChapterUrl;
        this.nextPageUrl = nextPageUrl;
        this.nextChapterUrl = nextChapterUrl;
        this.nextUrl = nextPageUrl == null ? nextChapterUrl : nextPageUrl;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public String getContent() {
        return content;
    }

    public String getPreviousChapterUrl() {
        return previousChapterUrl;
    }

    public String getNextPageUrl() {
        return nextPageUrl;
    }

    public String getNextChapterUrl() {
        return nextChapterUrl;
    }

    public String getNextUrl() {
        return nextUrl;
    }
}
