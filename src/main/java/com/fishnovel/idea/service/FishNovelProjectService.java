package com.fishnovel.idea.service;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.BookShelfItem;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.parser.BookParserRegistry;
import com.fishnovel.idea.parser.HtmlBookParser;
import com.fishnovel.idea.parser.RemoteHtmlBookCrawler;
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
    private final RemoteHtmlBookCrawler remoteHtmlBookCrawler;

    public FishNovelProjectService(Project project) {
        this.parserRegistry = BookParserRegistry.defaultRegistry();
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.remoteHtmlBookCrawler = new RemoteHtmlBookCrawler(new HtmlBookParser());
    }

    public BookDocument importBook(Path path) throws IOException {
        BookDocument document = parserRegistry.parse(path);
        ReadingStateService.getInstance().registerBook(document);
        return document;
    }

    public BookDocument importBookFromUrl(String url) throws IOException {
        try {
            BookDocument document = remoteHtmlBookCrawler.crawl(url, currentUrl -> {
                try {
                    return loadRemotePage(currentUrl);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("网页导入被中断。", ex);
                }
            });
            ReadingStateService.getInstance().registerBook(document);
            return document;
        } catch (IllegalArgumentException ex) {
            throw new IOException("无效的网页地址：" + url, ex);
        }
    }

    public BookDocument reopen(BookShelfItem item) throws IOException {
        if (item.getSourceType() == SourceType.REMOTE_URL) {
            return importBookFromUrl(item.getSourceLocation());
        }
        Path path = LocalBookPathResolver.resolve(item.getSourceLocation());
        if (!Files.exists(path)) {
            throw new IOException("找不到本地小说文件：" + item.getSourceLocation());
        }
        return importBook(path);
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
            throw new IOException("网页请求失败，状态码：" + response.statusCode());
        }
        if (RemoteRedirectPolicy.isUnexpectedRedirect(requestUri, response.uri())) {
            throw new IOException("网页被重定向到了其他站点：" + response.uri());
        }
        return response.body();
    }
}
