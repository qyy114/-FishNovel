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
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
            @Override
            public boolean isFileSelectable(VirtualFile file) {
                return file != null && !file.isDirectory() && isSupportedFile(file);
            }

            @Override
            public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                return file != null && (file.isDirectory() || isSupportedFile(file));
            }
        };
        descriptor.setTitle(title);
        descriptor.setDescription("支持 TXT、EPUB、Markdown、HTML（.html / .htm）");
        descriptor.setHideIgnored(false);
        return descriptor;
    }
}
