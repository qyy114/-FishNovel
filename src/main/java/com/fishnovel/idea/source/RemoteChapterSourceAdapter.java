package com.fishnovel.idea.source;

import com.fishnovel.idea.parser.RemoteHtmlBookCrawler;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

public interface RemoteChapterSourceAdapter {
    String getId();

    String getDisplayName();

    boolean supports(URI uri);

    RemoteChapterLoadResult loadChapter(URI uri, RemoteHtmlBookCrawler.RemotePageLoader loader) throws IOException;

    default Optional<String> resolveChapterJump(URI currentUri, int chapterNumber) {
        return Optional.empty();
    }
}
