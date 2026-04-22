package com.fishnovel.idea.source;

import com.fishnovel.idea.model.BookDocument;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class FanqiePlaceholderAdapter implements BookSourceAdapter, RemoteCatalogProvider {
    @Override
    public String getId() {
        return "fanqie";
    }

    @Override
    public String getDisplayName() {
        return "番茄小说（预留接入）";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<RemoteBookSummary> search(String keyword) {
        return Collections.emptyList();
    }

    @Override
    public Optional<BookDocument> fetchBook(String remoteId) throws IOException {
        return Optional.empty();
    }

    @Override
    public List<RemoteBookSummary> featuredBooks() {
        return List.of(new RemoteBookSummary(
            getId(),
            "coming-soon",
            "番茄小说接入开发中",
            "FishNovel",
            "当前版本仅预留接口，不包含真实抓取与登录流程。"
        ));
    }
}
