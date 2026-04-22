package com.fishnovel.idea.source;

import com.fishnovel.idea.model.BookDocument;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface BookSourceAdapter {
    String getId();

    String getDisplayName();

    boolean isAvailable();

    List<RemoteBookSummary> search(String keyword);

    Optional<BookDocument> fetchBook(String remoteId) throws IOException;
}
