package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class BookParserRegistry {
    private final List<BookParser> parsers;

    public BookParserRegistry(List<BookParser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    public static BookParserRegistry defaultRegistry() {
        return new BookParserRegistry(List.of(
            new TxtBookParser(),
            new MarkdownBookParser(),
            new HtmlBookParser(),
            new EpubBookParser()
        ));
    }

    public BookDocument parse(Path path) throws IOException {
        for (BookParser parser : parsers) {
            if (parser.supports(path)) {
                return parser.parse(path);
            }
        }
        throw new IOException("Unsupported book format: " + path.getFileName());
    }
}
