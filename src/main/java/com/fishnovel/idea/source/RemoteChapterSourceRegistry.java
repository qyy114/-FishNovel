package com.fishnovel.idea.source;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public final class RemoteChapterSourceRegistry {
    private final List<RemoteChapterSourceAdapter> adapters;

    public RemoteChapterSourceRegistry(List<RemoteChapterSourceAdapter> adapters) {
        this.adapters = List.copyOf(adapters);
    }

    public static RemoteChapterSourceRegistry defaultRegistry() {
        return new RemoteChapterSourceRegistry(List.of(
            new SuduguSourceAdapter(),
            new BqgAjaxSourceAdapter(),
            new GenericHtmlSourceAdapter()
        ));
    }

    public Optional<RemoteChapterSourceAdapter> findAdapter(URI uri) {
        return adapters.stream()
            .filter(adapter -> adapter.supports(uri))
            .findFirst();
    }

    public List<RemoteChapterSourceAdapter> getAdapters() {
        return adapters;
    }
}
