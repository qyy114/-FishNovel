package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.util.BookIdGenerator;
import com.fishnovel.idea.util.TextDecoders;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class HtmlBookParser implements BookParser {
    @Override
    public boolean supports(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".html") || name.endsWith(".htm");
    }

    @Override
    public BookDocument parse(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return parseHtmlDocument(
            TextDecoders.decode(bytes),
            BookIdGenerator.fromBytes(bytes),
            path.getFileName().toString(),
            SourceType.LOCAL_FILE,
            path.toAbsolutePath().toString(),
            "html",
            BookIdGenerator.fromBytes(bytes),
            path.toAbsolutePath()
        );
    }

    public BookDocument parseRemote(String url, byte[] bytes) {
        RemoteHtmlPage page = parseRemotePage(url, bytes);
        return new BookDocument(
            BookIdGenerator.fromBytes(url.getBytes(StandardCharsets.UTF_8)),
            page.getBookTitle(),
            SourceType.REMOTE_URL,
            url,
            "html",
            BookIdGenerator.fromBytes(bytes),
            null,
            List.of(new Chapter(0, page.getChapterTitle(), page.getContent(), 0))
        );
    }

    public RemoteHtmlPage parseRemotePage(String url, byte[] bytes) {
        String html = TextDecoders.decode(bytes);
        Document document = Jsoup.parse(html, url);
        String fallbackName = url.substring(url.lastIndexOf('/') + 1);
        if (fallbackName.isBlank()) {
            fallbackName = "web-novel";
        }

        String fullTitle = extractTitle(document, fallbackName);
        TitleParts titleParts = splitTitle(fullTitle);
        Element contentRoot = resolveContentRoot(document);
        String content = extractReadableText(contentRoot);

        return new RemoteHtmlPage(
            titleParts.bookTitle(),
            titleParts.chapterTitle(),
            content.isBlank() ? fullTitle : content,
            extractNextUrl(document)
        );
    }

    private BookDocument parseHtmlDocument(
        String html,
        String bookId,
        String fallbackName,
        SourceType sourceType,
        String sourceLocation,
        String fileExtension,
        String contentHash,
        Path sourcePath
    ) {
        Document document = Jsoup.parse(html, sourceLocation);
        String title = extractTitle(document, fallbackName);
        Element contentRoot = resolveContentRoot(document);
        List<Chapter> chapters = extractChapters(contentRoot, title);
        return new BookDocument(
            bookId,
            title,
            sourceType,
            sourceLocation,
            fileExtension,
            contentHash,
            sourcePath,
            chapters
        );
    }

    private String extractTitle(Document document, String fallbackName) {
        String metaTitle = document.select("meta[property=og:title], meta[name=title]").attr("content").trim();
        if (!metaTitle.isBlank()) {
            return metaTitle;
        }
        Element heading = document.selectFirst("h1, .bookname h1, .article-title, .chaptertitle");
        if (heading != null && !heading.text().isBlank()) {
            return heading.text().trim();
        }
        return document.title().isBlank() ? stripExtension(fallbackName) : document.title();
    }

    private TitleParts splitTitle(String title) {
        String normalized = title == null ? "" : title.replace('＞', '>').trim();
        if (normalized.contains(">")) {
            String[] parts = normalized.split("\\s*>\\s*", 2);
            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                return new TitleParts(parts[0].trim(), parts[1].trim());
            }
        }
        return new TitleParts(
            normalized.isBlank() ? "网页小说" : normalized,
            normalized.isBlank() ? "正文" : normalized
        );
    }

    private Element resolveContentRoot(Document document) {
        Element root = document.selectFirst(
            "#chaptercontent, .chapter-content, .chapterContent, .con, .content, .txtnav, .yd_text2, .Readarea, .read-content, .bookcontent, .novelcontent, #content1, #contenttxt, article, main, #content"
        );
        if (root != null) {
            return root;
        }
        return document.body() == null ? document : document.body();
    }

    private List<Chapter> extractChapters(Element contentRoot, String fallbackTitle) {
        Elements headings = contentRoot.select("h1, h2, h3");
        if (headings.isEmpty()) {
            String bodyText = extractReadableText(contentRoot);
            return List.of(new Chapter(0, fallbackTitle, bodyText.isBlank() ? fallbackTitle : bodyText, 0));
        }

        List<Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < headings.size(); i++) {
            Element heading = headings.get(i);
            StringBuilder builder = new StringBuilder();
            builder.append(heading.text()).append("\n\n");
            for (Element sibling = heading.nextElementSibling(); sibling != null; sibling = sibling.nextElementSibling()) {
                if (sibling.tagName().matches("h1|h2|h3")) {
                    break;
                }
                String text = sibling.text().trim();
                if (!text.isBlank()) {
                    builder.append(text).append("\n\n");
                }
            }
            chapters.add(new Chapter(i, heading.text(), builder.toString().trim(), i));
        }
        return chapters;
    }

    private String extractReadableText(Element contentRoot) {
        Elements paragraphs = contentRoot.select("p, div, section, article, br");
        if (paragraphs.isEmpty()) {
            return contentRoot.text().trim();
        }
        StringBuilder builder = new StringBuilder();
        for (Element paragraph : paragraphs) {
            String text = paragraph.text().trim();
            if (!text.isBlank() && text.length() > 1) {
                builder.append(text).append("\n\n");
            }
        }
        String content = builder.toString().trim();
        return content.isBlank() ? contentRoot.text().trim() : content;
    }

    private String extractNextUrl(Document document) {
        Element nextLink = document.selectFirst("a:matchesOwn((?i)^\\s*(下一页|下一章|下页|下章|next|next chapter)\\s*$)");
        if (nextLink == null) {
            nextLink = document.selectFirst("a[rel=next], .next a, a.next");
        }
        if (nextLink == null) {
            return null;
        }
        String href = nextLink.absUrl("href").trim();
        return href.isBlank() ? null : href;
    }

    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private record TitleParts(String bookTitle, String chapterTitle) {
    }
}
