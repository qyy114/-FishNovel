package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import java.io.IOException;
import java.nio.file.Path;

public interface BookParser {
    boolean supports(Path path);

    BookDocument parse(Path path) throws IOException;
}
