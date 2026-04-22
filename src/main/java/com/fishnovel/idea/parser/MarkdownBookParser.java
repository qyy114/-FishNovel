package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.util.BookIdGenerator;
import com.fishnovel.idea.util.TextDecoders;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;

public final class MarkdownBookParser implements BookParser {
    private static final Pattern HEADING_PATTERN = Pattern.compile("(?m)^(#{1,6})\\s+(.+)$");
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    @Override
    public boolean supports(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".md") || name.endsWith(".markdown");
    }

    @Override
    public BookDocument parse(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String markdown = TextDecoders.decode(bytes).replace("\r\n", "\n");
        List<Chapter> chapters = splitSections(markdown, stripExtension(path.getFileName().toString()));
        return new BookDocument(
            BookIdGenerator.fromBytes(bytes),
            stripExtension(path.getFileName().toString()),
            SourceType.LOCAL_FILE,
            path.toAbsolutePath().toString(),
            "md",
            BookIdGenerator.fromBytes(bytes),
            path.toAbsolutePath(),
            chapters
        );
    }

    private List<Chapter> splitSections(String markdown, String fallbackTitle) {
        Matcher matcher = HEADING_PATTERN.matcher(markdown);
        List<Integer> starts = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
            titles.add(matcher.group(2).trim());
        }

        if (starts.isEmpty()) {
            return List.of(new Chapter(0, fallbackTitle, markdownToPlain(markdown), 0));
        }

        List<Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = i + 1 < starts.size() ? starts.get(i + 1) : markdown.length();
            String section = markdown.substring(start, end).trim();
            chapters.add(new Chapter(i, titles.get(i), markdownToPlain(section), start));
        }
        return chapters;
    }

    private String markdownToPlain(String markdown) {
        Node node = markdownParser.parse(markdown);
        String html = htmlRenderer.render(node);
        return Jsoup.parse(html).text();
    }

    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }
}
