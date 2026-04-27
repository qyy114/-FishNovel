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

public final class TxtBookParser implements BookParser {
    private static final String CHAPTER_NUMBER = "0-9\\u96f6\\u3007\\u4e00\\u4e8c\\u4e24\\u4e09\\u56db\\u4e94\\u516d\\u4e03\\u516b\\u4e5d\\u5341\\u767e\\u5343\\u4e07";
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
        "(?m)^\\s*((?:\\u7b2c\\s*[" + CHAPTER_NUMBER + "]{1,20}\\s*[\\u7ae0\\u8282\\u56de\\u8bdd\\u5377\\u7bc7\\u90e8\\u96c6]"
            + "|\\u5377\\s*[" + CHAPTER_NUMBER + "]{1,20}).*)$"
    );

    @Override
    public boolean supports(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".txt");
    }

    @Override
    public BookDocument parse(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String text = TextDecoders.decode(bytes).replace("\r\n", "\n");
        List<Chapter> chapters = splitChapters(text, stripExtension(path.getFileName().toString()));
        return new BookDocument(
            BookIdGenerator.fromBytes(bytes),
            stripExtension(path.getFileName().toString()),
            SourceType.LOCAL_FILE,
            path.toAbsolutePath().toString(),
            "txt",
            BookIdGenerator.fromBytes(bytes),
            path.toAbsolutePath(),
            chapters
        );
    }

    private List<Chapter> splitChapters(String text, String fallbackTitle) {
        Matcher matcher = CHAPTER_PATTERN.matcher(text);
        List<Integer> starts = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
            titles.add(matcher.group(1).trim());
        }

        if (starts.isEmpty()) {
            return List.of(new Chapter(0, fallbackTitle, text.trim(), 0));
        }

        List<Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = i + 1 < starts.size() ? starts.get(i + 1) : text.length();
            String content = text.substring(start, end).trim();
            chapters.add(new Chapter(i, titles.get(i), content, start));
        }
        return chapters;
    }

    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }
}
