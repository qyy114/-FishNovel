package com.fishnovel.idea.editor;

import com.fishnovel.idea.ui.BookReaderPanel;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FishNovelFileEditor extends UserDataHolderBase implements FileEditor {
    private final BookReaderPanel readerPanel;

    public FishNovelFileEditor(Project project, FishNovelVirtualFile file) {
        this.readerPanel = new BookReaderPanel(project, () -> {});
        this.readerPanel.openBook(file.getBookDocument(), file.getInitialProgress());
    }

    @Override
    public @NotNull JComponent getComponent() {
        return readerPanel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return readerPanel.getPreferredFocusedComponent();
    }

    @Override
    public @NotNull String getName() {
        return "FishNovel Reader";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public void dispose() {
        readerPanel.disposePanel();
    }
}
