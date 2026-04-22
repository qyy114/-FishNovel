package com.fishnovel.idea.action;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.service.FishNovelProjectService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public final class ImportBookAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        descriptor.setTitle("导入小说到 FishNovel");
        descriptor.withFileFilter(file -> {
            String name = file.getName().toLowerCase();
            return name.endsWith(".txt")
                || name.endsWith(".epub")
                || name.endsWith(".md")
                || name.endsWith(".markdown")
                || name.endsWith(".html")
                || name.endsWith(".htm");
        });

        VirtualFile selectedFile = FileChooser.chooseFile(descriptor, project, null);
        if (selectedFile == null) {
            return;
        }

        try {
            BookDocument document = FishNovelProjectService.getInstance(project).importBook(Path.of(selectedFile.getPath()));
            FishNovelProjectService.getInstance(project).openInEditor(document);
        } catch (IOException ex) {
            Messages.showErrorDialog(project, "导入小说失败：\n" + ex.getMessage(), "FishNovel");
        }
    }
}
