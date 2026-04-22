package com.fishnovel.idea.editor;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.ReadingProgress;
import com.intellij.testFramework.LightVirtualFile;

public final class FishNovelVirtualFile extends LightVirtualFile {
    private final BookDocument bookDocument;
    private final ReadingProgress initialProgress;

    public FishNovelVirtualFile(BookDocument bookDocument) {
        this(bookDocument, null);
    }

    public FishNovelVirtualFile(BookDocument bookDocument, ReadingProgress initialProgress) {
        super(bookDocument.getTitle() + ".fishnovel");
        this.bookDocument = bookDocument;
        this.initialProgress = initialProgress;
        setWritable(false);
    }

    public BookDocument getBookDocument() {
        return bookDocument;
    }

    public ReadingProgress getInitialProgress() {
        return initialProgress;
    }
}
