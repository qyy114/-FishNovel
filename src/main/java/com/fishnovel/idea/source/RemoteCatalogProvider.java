package com.fishnovel.idea.source;

import java.util.List;

public interface RemoteCatalogProvider {
    List<RemoteBookSummary> featuredBooks();
}
