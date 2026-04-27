package com.fishnovel.idea.service;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.BookShelfItem;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.parser.BookParserRegistry;
import com.fishnovel.idea.parser.TxtBookParser;
import com.fishnovel.idea.source.RemoteChapterLoadResult;
import com.fishnovel.idea.source.RemoteChapterSourceAdapter;
import com.fishnovel.idea.source.RemoteChapterSourceRegistry;
import com.fishnovel.idea.source.TomatoDownloadResult;
import com.fishnovel.idea.source.TomatoSourceLocation;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

@Service(Service.Level.PROJECT)
public final class FishNovelProjectService {
    private static final String USER_AGENT = "FishNovel/0.1.4 IntelliJ Plugin";

    private final BookParserRegistry parserRegistry;
    private final HttpClient httpClient;
    private final RemoteChapterSourceRegistry remoteSourceRegistry;
    private final TomatoDownloaderService tomatoDownloaderService;

    public FishNovelProjectService(Project project) {
        this.parserRegistry = BookParserRegistry.defaultRegistry();
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.remoteSourceRegistry = RemoteChapterSourceRegistry.defaultRegistry();
        this.tomatoDownloaderService = new TomatoDownloaderService(ReadingStateService.getInstance());
    }

    public BookDocument importBook(Path path) throws IOException {
        BookDocument document = parserRegistry.parse(path);
        ReadingStateService.getInstance().registerBook(document);
        return document;
    }

    public BookDocument importBookFromUrl(String url) throws IOException {
        return importRemoteChapterFromUrl(url).getDocument();
    }

    public RemoteChapterLoadResult importRemoteChapterFromUrl(String url) throws IOException {
        try {
            URI requestUri = URI.create(url);
            RemoteChapterSourceAdapter adapter = remoteSourceRegistry.findAdapter(requestUri)
                .orElseThrow(() -> new IOException("Unsupported web page URL: " + url));
            RemoteChapterLoadResult result = adapter.loadChapter(requestUri, currentUrl -> {
                try {
                    return loadRemotePage(currentUrl);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Web page loading was interrupted.", ex);
                }
            });
            ReadingStateService.getInstance().registerBook(result.getDocument());
            return result;
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid web page URL: " + url, ex);
        }
    }

    public RemoteChapterLoadResult importRemoteChapterByNumber(String currentUrl, int chapterNumber) throws IOException {
        try {
            URI currentUri = URI.create(currentUrl);
            RemoteChapterSourceAdapter adapter = remoteSourceRegistry.findAdapter(currentUri)
                .orElseThrow(() -> new IOException("Current web source does not support chapter jump."));
            String targetUrl = adapter.resolveChapterJump(currentUri, chapterNumber)
                .orElseThrow(() -> new IOException("Current web source does not support chapter jump."));
            return importRemoteChapterFromUrl(targetUrl);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid current web chapter URL: " + currentUrl, ex);
        }
    }

    public boolean hasValidTomatoDownloaderPath() {
        return tomatoDownloaderService.hasValidDownloaderPath();
    }

    public void setTomatoDownloaderPath(Path path) {
        tomatoDownloaderService.setDownloaderPath(path);
    }

    public BookDocument importTomatoBook(String input) throws IOException {
        return parseTomatoDownload(tomatoDownloaderService.download(input));
    }

    public BookDocument refreshTomatoBook(BookDocument document) throws IOException {
        if (document.getSourceType() != SourceType.TOMATO_TXT) {
            throw new IOException("Current book is not a Tomato TXT book.");
        }
        String bookId = TomatoSourceLocation.parseBookId(document.getSourceLocation())
            .orElseThrow(() -> new IOException("Invalid Tomato book source: " + document.getSourceLocation()));
        return parseTomatoDownload(tomatoDownloaderService.refresh(bookId));
    }

    public BookDocument reopen(BookShelfItem item) throws IOException {
        if (item.getSourceType() == SourceType.REMOTE_URL) {
            return importBookFromUrl(item.getSourceLocation());
        }
        if (item.getSourceType() == SourceType.TOMATO_TXT) {
            String bookId = TomatoSourceLocation.parseBookId(item.getSourceLocation())
                .orElseThrow(() -> new IOException("Invalid Tomato book source: " + item.getSourceLocation()));
            return parseTomatoDownload(tomatoDownloaderService.cached(bookId));
        }
        Path path = LocalBookPathResolver.resolve(item.getSourceLocation());
        if (!Files.exists(path)) {
            throw new IOException("找不到本地小说文件：" + item.getSourceLocation());
        }
        return importBook(path);
    }

    private BookDocument parseTomatoDownload(TomatoDownloadResult result) throws IOException {
        BookDocument document = new TxtBookParser().parseTomato(result.getTxtPath(), result.getBookId(), result.getTitle());
        ReadingStateService.getInstance().registerBook(document);
        return document;
    }

    public static FishNovelProjectService getInstance(Project project) {
        return project.getService(FishNovelProjectService.class);
    }

    private byte[] loadRemotePage(String url) throws IOException, InterruptedException {
        URI requestUri = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder(requestUri)
            .GET()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .header("Referer", url)
            .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            throw new IOException("Web page request failed with status code: " + response.statusCode());
        }
        if (RemoteRedirectPolicy.isUnexpectedRedirect(requestUri, response.uri())) {
            throw new IOException("Web page was redirected to another site: " + response.uri());
        }
        return response.body();
    }
}
