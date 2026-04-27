package com.fishnovel.idea.source;

import java.net.URI;
import java.util.Optional;

public final class TomatoSourceLocation {
    private static final String SCHEME = "tomato";
    private static final String HOST = "book";

    private TomatoSourceLocation() {
    }

    public static String toLocation(String bookId) {
        String normalizedBookId = normalizeBookId(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Tomato book id must be numeric."));
        return SCHEME + "://" + HOST + "/" + normalizedBookId;
    }

    public static Optional<String> parseBookId(String sourceLocation) {
        if (sourceLocation == null || sourceLocation.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(sourceLocation.trim());
            if (!SCHEME.equalsIgnoreCase(uri.getScheme()) || !HOST.equalsIgnoreCase(uri.getHost())) {
                return Optional.empty();
            }
            String path = uri.getPath();
            if (path == null || path.length() <= 1) {
                return Optional.empty();
            }
            return normalizeBookId(path.substring(1));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static Optional<String> normalizeBookId(String bookId) {
        if (bookId == null) {
            return Optional.empty();
        }
        String trimmed = bookId.trim();
        if (trimmed.isEmpty() || !trimmed.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
    }
}
