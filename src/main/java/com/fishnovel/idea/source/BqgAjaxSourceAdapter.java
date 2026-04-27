package com.fishnovel.idea.source;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.parser.RemoteHtmlBookCrawler;
import com.fishnovel.idea.util.BookIdGenerator;
import com.fishnovel.idea.util.TextDecoders;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BqgAjaxSourceAdapter implements RemoteChapterSourceAdapter {
    private static final Pattern CHAPTER_PATH = Pattern.compile("^/book/(\\d+)/(\\d+)(?:_\\d+)?\\.html$");

    @Override
    public String getId() {
        return "bqg-ajax";
    }

    @Override
    public String getDisplayName() {
        return "Bqg AJAX";
    }

    @Override
    public boolean supports(URI uri) {
        if (uri == null || uri.getHost() == null || uri.getPath() == null) {
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return host.contains("bqg") && CHAPTER_PATH.matcher(uri.getPath()).matches();
    }

    @Override
    public RemoteChapterLoadResult loadChapter(URI uri, RemoteHtmlBookCrawler.RemotePageLoader loader) throws IOException {
        Matcher matcher = CHAPTER_PATH.matcher(uri.getPath());
        if (!matcher.matches()) {
            throw new IOException("Unsupported BQG chapter URL: " + uri);
        }

        int bookId = Integer.parseInt(matcher.group(1));
        int chapterId = Integer.parseInt(matcher.group(2));
        URI apiUri = uri.resolve("/api/chapter?id=" + bookId + "&chapterid=" + chapterId);
        String json = TextDecoders.decode(loader.load(apiUri.toString()));
        ChapterPayload payload = ChapterPayload.parse(json);
        String currentUrl = chapterUrl(uri, bookId, chapterId);
        String previousUrl = chapterId > 1 ? chapterUrl(uri, bookId, chapterId - 1) : null;
        String nextUrl = payload.chapterCount() > chapterId ? chapterUrl(uri, bookId, chapterId + 1) : null;

        BookDocument document = new BookDocument(
            BookIdGenerator.fromBytes((uri.getHost() + "/book/" + bookId + "#" + payload.title()).getBytes(StandardCharsets.UTF_8)),
            payload.title(),
            SourceType.REMOTE_URL,
            currentUrl,
            "html",
            BookIdGenerator.fromBytes((apiUri + "#" + payload.text().length()).getBytes(StandardCharsets.UTF_8)),
            null,
            List.of(new Chapter(0, payload.chapterName(), payload.text(), 0))
        );
        return new RemoteChapterLoadResult(
            document,
            new RemoteChapterNavigation(currentUrl, previousUrl, nextUrl),
            getId(),
            null
        );
    }

    @Override
    public Optional<String> resolveChapterJump(URI currentUri, int chapterNumber) {
        if (chapterNumber <= 0 || currentUri == null || currentUri.getPath() == null) {
            return Optional.empty();
        }
        Matcher matcher = CHAPTER_PATH.matcher(currentUri.getPath());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        int bookId = Integer.parseInt(matcher.group(1));
        return Optional.of(chapterUrl(currentUri, bookId, chapterNumber));
    }

    private String chapterUrl(URI baseUri, int bookId, int chapterId) {
        return baseUri.resolve("/book/" + bookId + "/" + chapterId + ".html").toString();
    }

    private record ChapterPayload(String title, String chapterName, String text, int chapterCount) {
        private static ChapterPayload parse(String json) throws IOException {
            String title = requireString(json, "title");
            String chapterName = requireString(json, "chaptername");
            String text = requireString(json, "txt");
            int chapterCount = requireInt(json, "cs");
            if (text.isBlank()) {
                throw new IOException("BQG chapter API returned empty content.");
            }
            return new ChapterPayload(title, chapterName, text, chapterCount);
        }

        private static String requireString(String json, String key) throws IOException {
            int valueStart = findValueStart(json, key);
            if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
                throw new IOException("BQG chapter API response is missing `" + key + "`.");
            }
            return readJsonString(json, valueStart);
        }

        private static int requireInt(String json, String key) throws IOException {
            int valueStart = findValueStart(json, key);
            if (valueStart >= json.length()) {
                throw new IOException("BQG chapter API response is missing `" + key + "`.");
            }
            boolean quoted = json.charAt(valueStart) == '"';
            int digitStart = quoted ? valueStart + 1 : valueStart;
            int digitEnd = digitStart;
            while (digitEnd < json.length() && Character.isDigit(json.charAt(digitEnd))) {
                digitEnd++;
            }
            if (digitStart == digitEnd || (quoted && (digitEnd >= json.length() || json.charAt(digitEnd) != '"'))) {
                throw new IOException("BQG chapter API response has invalid `" + key + "`.");
            }
            String value = json.substring(digitStart, digitEnd);
            return Integer.parseInt(value);
        }

        private static int findValueStart(String json, String key) throws IOException {
            String quotedKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(quotedKey);
            if (keyIndex < 0) {
                throw new IOException("BQG chapter API response is missing `" + key + "`.");
            }
            int colonIndex = json.indexOf(':', keyIndex + quotedKey.length());
            if (colonIndex < 0) {
                throw new IOException("BQG chapter API response has invalid `" + key + "`.");
            }
            int valueStart = colonIndex + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }
            return valueStart;
        }

        private static String readJsonString(String json, int quoteIndex) throws IOException {
            StringBuilder builder = new StringBuilder();
            for (int i = quoteIndex + 1; i < json.length(); i++) {
                char current = json.charAt(i);
                if (current == '"') {
                    return builder.toString().trim();
                }
                if (current != '\\') {
                    builder.append(current);
                    continue;
                }
                if (i + 1 >= json.length()) {
                    throw new IOException("Invalid JSON escape sequence.");
                }
                char escaped = json.charAt(++i);
                switch (escaped) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        if (i + 4 >= json.length()) {
                            throw new IOException("Invalid JSON unicode escape sequence.");
                        }
                        String hex = json.substring(i + 1, i + 5);
                        builder.append((char) Integer.parseInt(hex, 16));
                        i += 4;
                    }
                    default -> throw new IOException("Unsupported JSON escape sequence: \\" + escaped);
                }
            }
            throw new IOException("Unterminated JSON string.");
        }
    }
}
