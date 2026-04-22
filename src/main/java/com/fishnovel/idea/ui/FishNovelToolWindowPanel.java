package com.fishnovel.idea.ui;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.BookShelfItem;
import com.fishnovel.idea.model.Bookmark;
import com.fishnovel.idea.model.ReadingProgress;
import com.fishnovel.idea.model.RecentEntry;
import com.fishnovel.idea.service.FishNovelProjectService;
import com.fishnovel.idea.service.ReadingStateService;
import com.fishnovel.idea.source.BookSourceAdapter;
import com.fishnovel.idea.source.RemoteBookSummary;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

public final class FishNovelToolWindowPanel extends JPanel {
    private final Project project;
    private final FishNovelProjectService projectService;
    private final ReadingStateService stateService;
    private final BookReaderPanel readerPanel;
    private final DefaultListModel<BookShelfItem> libraryModel = new DefaultListModel<>();
    private final DefaultListModel<RecentEntry> recentModel = new DefaultListModel<>();
    private final DefaultListModel<Bookmark> bookmarkModel = new DefaultListModel<>();
    private final DefaultListModel<RemoteBookSummary> sourceModel = new DefaultListModel<>();
    private final JList<BookShelfItem> libraryList = new JList<>(libraryModel);
    private final JList<RecentEntry> recentList = new JList<>(recentModel);
    private final JList<Bookmark> bookmarkList = new JList<>(bookmarkModel);
    private final JList<RemoteBookSummary> sourceList = new JList<>(sourceModel);

    public FishNovelToolWindowPanel(Project project) {
        super(new BorderLayout(0, 12));
        this.project = project;
        this.projectService = FishNovelProjectService.getInstance(project);
        this.stateService = ReadingStateService.getInstance();
        this.readerPanel = new BookReaderPanel(project, this::refreshSidebar);

        buildUi();
        refreshSidebar();
    }

    private void buildUi() {
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton importButton = new JButton("导入小说");
        JButton openSelectedInEditorButton = new JButton("选中项在编辑器打开");
        JButton refreshButton = new JButton("刷新列表");

        importButton.addActionListener(event -> importBook());
        openSelectedInEditorButton.addActionListener(event -> openSelectedInEditor());
        refreshButton.addActionListener(event -> refreshSidebar());

        JPanel toolbar = new JPanel();
        toolbar.add(importButton);
        toolbar.add(openSelectedInEditorButton);
        toolbar.add(refreshButton);

        configureList(libraryList);
        configureList(recentList);
        configureList(bookmarkList);
        configureList(sourceList);

        libraryList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                BookShelfItem selected = libraryList.getSelectedValue();
                if (selected != null) {
                    openBookInPanel(selected);
                }
            }
        });

        recentList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                RecentEntry selected = recentList.getSelectedValue();
                if (selected != null) {
                    stateService.findBook(selected.getBookId()).ifPresent(this::openBookInPanel);
                }
            }
        });

        bookmarkList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                Bookmark selected = bookmarkList.getSelectedValue();
                if (selected != null) {
                    openBookmarkInPanel(selected);
                }
            }
        });

        addDoubleClick(libraryList, this::openBookInEditor);
        addDoubleClick(recentList, recent -> stateService.findBook(recent.getBookId()).ifPresent(this::openBookInEditor));
        addDoubleClick(bookmarkList, this::openBookmarkInEditor);

        libraryList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            DefaultListCellRenderer renderer = new DefaultListCellRenderer();
            renderer.setText(value.getTitle() + "  [" + value.getBookmarkCount() + " 书签]");
            return renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        });
        recentList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            DefaultListCellRenderer renderer = new DefaultListCellRenderer();
            renderer.setText(value.getTitle());
            return renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        });
        bookmarkList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            DefaultListCellRenderer renderer = new DefaultListCellRenderer();
            renderer.setText(value.getBookTitle() + " · " + value.getChapterTitle());
            return renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        });
        sourceList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            DefaultListCellRenderer renderer = new DefaultListCellRenderer();
            renderer.setText(value.getTitle() + " · " + value.getSummary());
            return renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        });

        JTabbedPane sidebar = new JTabbedPane();
        sidebar.addTab("书架", new JScrollPane(libraryList));
        sidebar.addTab("最近", new JScrollPane(recentList));
        sidebar.addTab("书签", new JScrollPane(bookmarkList));
        sidebar.addTab("番茄", new JScrollPane(sourceList));
        sidebar.setPreferredSize(new Dimension(280, 500));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, readerPanel);
        splitPane.setResizeWeight(0.33);

        add(toolbar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void configureList(JList<?> list) {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(28);
    }

    private <T> void addDoubleClick(JList<T> list, java.util.function.Consumer<T> consumer) {
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    T value = list.getSelectedValue();
                    if (value != null) {
                        consumer.accept(value);
                    }
                }
            }
        });
    }

    private void importBook() {
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
            BookDocument document = projectService.importBook(Path.of(selectedFile.getPath()));
            refreshSidebar();
            readerPanel.openBook(document);
        } catch (IOException ex) {
            Messages.showErrorDialog(project, "导入小说失败：\n" + ex.getMessage(), "FishNovel");
        }
    }

    private void openSelectedInEditor() {
        if (libraryList.getSelectedValue() != null) {
            openBookInEditor(libraryList.getSelectedValue());
            return;
        }
        if (recentList.getSelectedValue() != null) {
            stateService.findBook(recentList.getSelectedValue().getBookId()).ifPresent(this::openBookInEditor);
            return;
        }
        if (bookmarkList.getSelectedValue() != null) {
            openBookmarkInEditor(bookmarkList.getSelectedValue());
        }
    }

    private void openBookInPanel(BookShelfItem item) {
        try {
            readerPanel.openBook(projectService.reopen(item));
            refreshSidebar();
        } catch (IOException ex) {
            Messages.showErrorDialog(project, "打开小说失败：\n" + ex.getMessage(), "FishNovel");
        }
    }

    private void openBookInEditor(BookShelfItem item) {
        try {
            BookDocument document = projectService.reopen(item);
            projectService.openInEditor(document);
            refreshSidebar();
        } catch (IOException ex) {
            Messages.showErrorDialog(project, "在编辑器打开失败：\n" + ex.getMessage(), "FishNovel");
        }
    }

    private void openBookmarkInPanel(Bookmark bookmark) {
        stateService.findBook(bookmark.getBookId()).ifPresent(item -> {
            try {
                BookDocument document = projectService.reopen(item);
                readerPanel.openBook(document, new ReadingProgress(bookmark.getChapterIndex(), bookmark.getContentOffset(), System.currentTimeMillis()));
                refreshSidebar();
            } catch (IOException ex) {
                Messages.showErrorDialog(project, "跳转书签失败：\n" + ex.getMessage(), "FishNovel");
            }
        });
    }

    private void openBookmarkInEditor(Bookmark bookmark) {
        stateService.findBook(bookmark.getBookId()).ifPresent(item -> {
            try {
                BookDocument document = projectService.reopen(item);
                projectService.openInEditor(document, new ReadingProgress(bookmark.getChapterIndex(), bookmark.getContentOffset(), System.currentTimeMillis()));
                refreshSidebar();
            } catch (IOException ex) {
                Messages.showErrorDialog(project, "在编辑器跳转书签失败：\n" + ex.getMessage(), "FishNovel");
            }
        });
    }

    private void refreshSidebar() {
        refillModel(libraryModel, stateService.listBooks());
        refillModel(recentModel, stateService.listRecentEntries());
        refillModel(bookmarkModel, stateService.listBookmarks());
        List<RemoteBookSummary> placeholders = projectService.getRemoteSources().stream()
            .flatMap(source -> source instanceof com.fishnovel.idea.source.RemoteCatalogProvider provider
                ? provider.featuredBooks().stream()
                : java.util.stream.Stream.<RemoteBookSummary>empty())
            .toList();
        refillModel(sourceModel, placeholders);
    }

    private <T> void refillModel(DefaultListModel<T> model, List<T> values) {
        SwingUtilities.invokeLater(() -> {
            model.clear();
            for (T value : values) {
                model.addElement(value);
            }
        });
    }
}
