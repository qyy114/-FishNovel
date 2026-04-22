package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.util.BookIdGenerator;
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
        Document document = Jsoup.parse(new String(bytes, StandardCharsets.UTF_8));
        String title = document.title().isBlank() ? stripExtension(path.getFileName().toString()) : document.title();
        List<Chapter> chapters = extractChapters(document, title);
        return new BookDocument(
            BookIdGenerator.fromBytes(bytes),
            title,
            SourceType.LOCAL_FILE,
            path.toAbsolutePath().toString(),
            "html",
            BookIdGenerator.fromBytes(bytes),
            path.toAbsolutePath(),
            chapters
        );
    }

    private List<Chapter> extractChapters(Document document, String fallbackTitle) {
        Elements headings = document.select("h1, h2, h3");
        if (headings.isEmpty()) {
            String bodyText = document.body() == null ? document.text() : document.body().text();
            return List.of(new Chapter(0, fallbackTitle, bodyText, 0));
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
                String text = sibling.text();
                if (!text.isBlank()) {
                    builder.append(text).append("\n\n");
                }
            }
            chapters.add(new Chapter(i, heading.text(), builder.toString().trim(), i));
        }
        return chapters;
    }

    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }
}
