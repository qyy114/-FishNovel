package com.fishnovel.idea.source;

public final class RemoteBookSummary {
    private final String sourceId;
    private final String remoteId;
    private final String title;
    private final String author;
    private final String summary;

    public RemoteBookSummary(String sourceId, String remoteId, String title, String author, String summary) {
        this.sourceId = sourceId;
        this.remoteId = remoteId;
        this.title = title;
        this.author = author;
        this.summary = summary;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getRemoteId() {
        return remoteId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getSummary() {
        return summary;
    }
}
