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
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("(?m)^(第.{1,20}[章节回卷篇部集].*)$");

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
