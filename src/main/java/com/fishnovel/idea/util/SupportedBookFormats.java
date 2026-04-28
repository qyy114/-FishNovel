package com.fishnovel.idea.util;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.List;
import java.util.Locale;

public final class SupportedBookFormats {
    public static final List<String> EXTENSIONS = List.of("txt", "epub", "md", "markdown", "html", "htm");

    private SupportedBookFormats() {
    }

    public static boolean isSupportedFileName(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        return EXTENSIONS.stream().anyMatch(extension -> lowerName.endsWith("." + extension));
    }

    public static boolean isSupportedFile(VirtualFile file) {
        return file != null && (file.isDirectory() || isSupportedFileName(file.getName()));
    }

    public static FileChooserDescriptor createImportDescriptor(String title) {
        return new FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle(title)
            .withDescription("支持 TXT、EPUB、Markdown、HTML（.html / .htm）")
            .withHideIgnored(false)
            .withFileFilter(SupportedBookFormats::isSupportedFile);
    }
}
