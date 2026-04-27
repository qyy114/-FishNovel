package com.fishnovel.idea.source;

public final class RemoteChapterNavigation {
    private final String currentUrl;
    private final String previousUrl;
    private final String nextUrl;

    public RemoteChapterNavigation(String currentUrl, String previousUrl, String nextUrl) {
        this.currentUrl = currentUrl;
        this.previousUrl = blankToNull(previousUrl);
        this.nextUrl = blankToNull(nextUrl);
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public String getPreviousUrl() {
        return previousUrl;
    }

    public String getNextUrl() {
        return nextUrl;
    }

    public boolean hasPrevious() {
        return previousUrl != null;
    }

    public boolean hasNext() {
        return nextUrl != null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
