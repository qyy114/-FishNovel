package com.fishnovel.idea.parser;

public final class RemoteHtmlPage {
    private final String bookTitle;
    private final String chapterTitle;
    private final String content;
    private final String nextUrl;

    public RemoteHtmlPage(String bookTitle, String chapterTitle, String content, String nextUrl) {
        this.bookTitle = bookTitle;
        this.chapterTitle = chapterTitle;
        this.content = content;
        this.nextUrl = nextUrl;
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

    public String getNextUrl() {
        return nextUrl;
    }
}
