package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.util.BookIdGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.jsoup.Jsoup;

public final class EpubBookParser implements BookParser {
    @Override
    public boolean supports(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".epub");
    }

    @Override
    public BookDocument parse(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        Book epubBook;
        try (InputStream inputStream = Files.newInputStream(path)) {
            epubBook = new EpubReader().readEpub(inputStream);
        }

        String title = epubBook.getTitle();
        if (title == null || title.isBlank()) {
            title = stripExtension(path.getFileName().toString());
        }

        List<Chapter> chapters = new ArrayList<>();
        int index = 0;
        for (SpineReference spineReference : epubBook.getSpine().getSpineReferences()) {
            Resource resource = spineReference.getResource();
            if (resource == null || resource.getData() == null) {
                continue;
            }
            String html = new String(resource.getData(), StandardCharsets.UTF_8);
            String chapterTitle = resource.getTitle();
            if (chapterTitle == null || chapterTitle.isBlank()) {
                chapterTitle = resource.getHref();
            }
            if (chapterTitle == null || chapterTitle.isBlank()) {
                chapterTitle = "Chapter " + (index + 1);
            }
            String text = Jsoup.parse(html).text();
            if (!text.isBlank()) {
                chapters.add(new Chapter(index, chapterTitle, text, index));
                index++;
            }
        }

        if (chapters.isEmpty()) {
            chapters.add(new Chapter(0, title, "", 0));
        }

        return new BookDocument(
            BookIdGenerator.fromBytes(bytes),
            title,
            SourceType.LOCAL_FILE,
            path.toAbsolutePath().toString(),
            "epub",
            BookIdGenerator.fromBytes(bytes),
            path.toAbsolutePath(),
            chapters
        );
    }

    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }
}
