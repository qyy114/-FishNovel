package com.fishnovel.idea.source;

import com.fishnovel.idea.model.BookDocument;

public final class RemoteChapterLoadResult {
    private final BookDocument document;
    private final RemoteChapterNavigation navigation;
    private final String sourceId;
    private final String warning;

    public RemoteChapterLoadResult(
        BookDocument document,
        RemoteChapterNavigation navigation,
        String sourceId,
        String warning
    ) {
        this.document = document;
        this.navigation = navigation;
        this.sourceId = sourceId;
        this.warning = warning == null || warning.isBlank() ? null : warning;
    }

    public BookDocument getDocument() {
        return document;
    }

    public RemoteChapterNavigation getNavigation() {
        return navigation;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getWarning() {
        return warning;
    }

    public boolean hasWarning() {
        return warning != null;
    }
}
