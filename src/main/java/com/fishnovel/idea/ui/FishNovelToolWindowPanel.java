package com.fishnovel.idea.ui;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.BookShelfItem;
import com.fishnovel.idea.model.Bookmark;
import com.fishnovel.idea.model.ReadingProgress;
import com.fishnovel.idea.model.RecentEntry;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.service.FishNovelProjectService;
import com.fishnovel.idea.service.ReadingStateService;
import com.fishnovel.idea.source.RemoteChapterLoadResult;
import com.fishnovel.idea.util.SupportedBookFormats;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public final class FishNovelToolWindowPanel extends JPanel implements Disposable {
    private static final String SECTION_LIBRARY = "library";
    private static final String SECTION_RECENT = "recent";
    private static final String SECTION_BOOKMARKS = "bookmarks";
    private static final int SIDEBAR_EXPANDED_WIDTH = 248;
    private static final int SIDEBAR_COLLAPSED_WIDTH = 30;

    private final Project project;
    private final ToolWindow toolWindow;
    private final FishNovelProjectService projectService;
    private final ReadingStateService stateService;
    private final BookReaderPanel readerPanel;
    private final KeyEventDispatcher bossKeyDispatcher = this::dispatchBossKey;
    private final DefaultListModel<BookShelfItem> libraryModel = new DefaultListModel<>();
    private final DefaultListModel<RecentEntry> recentModel = new DefaultListModel<>();
    private final DefaultListModel<Bookmark> bookmarkModel = new DefaultListModel<>();
    private final JBList<BookShelfItem> libraryList = new JBList<>(libraryModel);
    private final JBList<RecentEntry> recentList = new JBList<>(recentModel);
    private final JBList<Bookmark> bookmarkList = new JBList<>(bookmarkModel);
    private final ButtonGroup sectionButtonGroup = new ButtonGroup();
    private final JPanel importToolbarActions = new JPanel();
    private final JPanel sidebarSections = new JPanel();
    private final JPanel librarySectionContent = new JPanel(new BorderLayout());
    private final JPanel recentSectionContent = new JPanel(new BorderLayout());
    private final JPanel bookmarkSectionContent = new JPanel(new BorderLayout());

    private JSplitPane splitPane;
    private JPanel sidebar;
    private JButton topToolbarsToggleButton;
    private JButton sidebarToggleButton;
    private JToggleButton librarySectionButton;
    private JToggleButton recentSectionButton;
    private JToggleButton bookmarkSectionButton;
    private String activeSection = SECTION_LIBRARY;
    private boolean topToolbarsCollapsed = true;
    private boolean sidebarCollapsed;
    private boolean suppressLibraryOpen;
    private BookShelfItem libraryPopupTarget;

    public FishNovelToolWindowPanel(Project project, ToolWindow toolWindow) {
        super(new BorderLayout(0, 12));
        this.project = project;
        this.toolWindow = toolWindow;
        this.projectService = FishNovelProjectService.getInstance(project);
        this.stateService = ReadingStateService.getInstance();
        this.readerPanel = new BookReaderPanel(project, this::refreshSidebar);

        buildUi();
        installBossKeyDispatcher();
        refreshSidebar();
    }

    private void installBossKeyDispatcher() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(bossKeyDispatcher);
    }

    private boolean dispatchBossKey(KeyEvent event) {
        if (event.getID() != KeyEvent.KEY_PRESSED || !isShowing() || !toolWindow.isVisible()) {
            return false;
        }
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (activeWindow instanceof Dialog) {
            return false;
        }
        toolWindow.hide(null);
        return true;
    }

    @Override
    public void dispose() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(bossKeyDispatcher);
        readerPanel.disposePanel();
    }

    private void buildUi() {
        setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 6));
        setBackground(UIUtil.getPanelBackground());

        JButton importButton = createToolbarButton("导入小说");
        JButton onlineReadButton = createToolbarButton("在线阅读");
        importButton.addActionListener(event -> importBook());
        onlineReadButton.addActionListener(event -> importWebBook());

        JPanel toolbar = new JPanel(new BorderLayout(4, 0));
        toolbar.setOpaque(false);
        topToolbarsToggleButton = createTopToolbarsToggleButton();
        importToolbarActions.setOpaque(false);
        importToolbarActions.add(importButton);
        importToolbarActions.add(onlineReadButton);
        toolbar.add(topToolbarsToggleButton, BorderLayout.WEST);
        toolbar.add(importToolbarActions, BorderLayout.CENTER);
        updateTopToolbarsCollapsedState();

        configureList(libraryList, "暂无书架");
        configureList(recentList, "暂无最近阅读");
        configureList(bookmarkList, "暂无书签");
        installLibraryPopupMenu();
        installBookmarkPopupMenu();
        installBookmarkKeyboardDelete();

        libraryList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !suppressLibraryOpen) {
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

        libraryList.setCellRenderer((list, value, index, isSelected, cellHasFocus) ->
            createSidebarItem(value.getTitle(), shortenLocation(value.getSourceLocation()), value.getBookmarkCount() + " 个书签", isSelected)
        );
        recentList.setCellRenderer((list, value, index, isSelected, cellHasFocus) ->
            createSidebarItem(value.getTitle(), shortenLocation(value.getSourceLocation()), "最近阅读", isSelected)
        );
        bookmarkList.setCellRenderer((list, value, index, isSelected, cellHasFocus) ->
            createSidebarItem(value.getBookTitle(), value.getChapterTitle(), "书签定位", isSelected)
        );

        sidebar = createSidebar();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, readerPanel);
        splitPane.setResizeWeight(0.22);
        configureSplitPane(splitPane);
        applySidebarCollapsedState();

        add(toolbar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 4));
        sidebar.setOpaque(false);
        sidebar.setBorder(JBUI.Borders.empty(2, 0, 0, 0));

        sidebarToggleButton = createSidebarToggleButton();
        JPanel sidebarHeader = new JPanel(new BorderLayout());
        sidebarHeader.setOpaque(false);
        sidebarHeader.add(sidebarToggleButton, BorderLayout.WEST);

        sidebarSections.setOpaque(false);
        sidebarSections.setLayout(new BoxLayout(sidebarSections, BoxLayout.Y_AXIS));

        configureSectionContent(librarySectionContent, libraryList);
        configureSectionContent(recentSectionContent, recentList);
        configureSectionContent(bookmarkSectionContent, bookmarkList);

        librarySectionButton = createSectionButton("书架", SECTION_LIBRARY, true);
        recentSectionButton = createSectionButton("最近", SECTION_RECENT, false);
        bookmarkSectionButton = createSectionButton("书签", SECTION_BOOKMARKS, false);

        sidebarSections.add(librarySectionButton);
        sidebarSections.add(librarySectionContent);
        sidebarSections.add(Box.createVerticalStrut(4));
        sidebarSections.add(recentSectionButton);
        sidebarSections.add(recentSectionContent);
        sidebarSections.add(Box.createVerticalStrut(4));
        sidebarSections.add(bookmarkSectionButton);
        sidebarSections.add(bookmarkSectionContent);

        sidebar.add(sidebarHeader, BorderLayout.NORTH);
        sidebar.add(sidebarSections, BorderLayout.CENTER);

        showSidebarSection(SECTION_LIBRARY);
        return sidebar;
    }

    private JButton createSidebarToggleButton() {
        JButton button = new JButton("\u2039");
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(JBUI.Borders.empty(4, 8));
        button.setPreferredSize(new Dimension(30, 28));
        button.setToolTipText("收起侧边栏");
        button.addActionListener(event -> toggleSidebarCollapsed());
        styleSubtleButton(button);
        return button;
    }

    private JButton createTopToolbarsToggleButton() {
        JButton button = new JButton("\u203a");
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(JBUI.Borders.empty(4, 8));
        button.setPreferredSize(new Dimension(30, 28));
        button.setToolTipText("展开导入和跳转功能栏");
        button.addActionListener(event -> toggleTopToolbarsCollapsed());
        styleSubtleButton(button);
        return button;
    }

    private void configureSectionContent(JPanel sectionContent, JList<?> list) {
        sectionContent.setOpaque(false);
        sectionContent.setBorder(JBUI.Borders.empty(2, 0, 6, 0));
        sectionContent.add(createListScrollPane(list), BorderLayout.CENTER);
        sectionContent.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    private JButton createToolbarButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 12f));
        button.setBorder(JBUI.Borders.empty(8, 14));
        button.setOpaque(true);
        styleSubtleButton(button);
        return button;
    }

    private void styleSubtleButton(JButton button) {
        Color panelBackground = UIUtil.getPanelBackground();
        Color labelColor = UIUtil.getLabelForeground();
        button.setBackground(mix(panelBackground, labelColor, 0.06f));
        button.setForeground(labelColor);
    }

    private void configureList(JBList<?> list, String emptyText) {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(54);
        list.setBorder(BorderFactory.createEmptyBorder());
        list.setBackground(UIUtil.getPanelBackground());
        list.getEmptyText().setText(emptyText);
    }

    private JToggleButton createSectionButton(String text, String sectionKey, boolean selected) {
        JToggleButton button = new JToggleButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setFocusPainted(false);
        button.setBorder(JBUI.Borders.empty(7, 10));
        button.setPreferredSize(new Dimension(220, 34));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.setOpaque(true);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        styleSectionButton(button, selected);
        button.addActionListener(event -> showSidebarSection(sectionKey));
        sectionButtonGroup.add(button);
        button.setSelected(selected);
        return button;
    }

    private void showSidebarSection(String sectionKey) {
        activeSection = sectionKey;
        boolean librarySelected = SECTION_LIBRARY.equals(sectionKey);
        boolean recentSelected = SECTION_RECENT.equals(sectionKey);
        boolean bookmarkSelected = SECTION_BOOKMARKS.equals(sectionKey);

        librarySectionContent.setVisible(librarySelected);
        recentSectionContent.setVisible(recentSelected);
        bookmarkSectionContent.setVisible(bookmarkSelected);

        refreshSectionButton(librarySectionButton, librarySelected);
        refreshSectionButton(recentSectionButton, recentSelected);
        refreshSectionButton(bookmarkSectionButton, bookmarkSelected);
        revalidate();
        repaint();
    }

    private void toggleSidebarCollapsed() {
        sidebarCollapsed = !sidebarCollapsed;
        applySidebarCollapsedState();
    }

    private void toggleTopToolbarsCollapsed() {
        topToolbarsCollapsed = !topToolbarsCollapsed;
        updateTopToolbarsCollapsedState();
    }

    private void updateTopToolbarsCollapsedState() {
        if (topToolbarsToggleButton == null) {
            return;
        }
        importToolbarActions.setVisible(!topToolbarsCollapsed);
        readerPanel.setControlsCollapsed(topToolbarsCollapsed);
        topToolbarsToggleButton.setText(topToolbarsCollapsed ? "\u203a" : "\u2039");
        topToolbarsToggleButton.setToolTipText(topToolbarsCollapsed ? "展开导入和跳转功能栏" : "收起导入和跳转功能栏");
        revalidate();
        repaint();
    }

    private void applySidebarCollapsedState() {
        if (sidebar == null || sidebarSections == null || sidebarToggleButton == null) {
            return;
        }
        int width = sidebarCollapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH;
        sidebarSections.setVisible(!sidebarCollapsed);
        sidebarToggleButton.setText(sidebarCollapsed ? "\u203a" : "\u2039");
        sidebarToggleButton.setToolTipText(sidebarCollapsed ? "展开侧边栏" : "收起侧边栏");
        sidebar.setPreferredSize(new Dimension(width, 520));
        sidebar.setMinimumSize(new Dimension(width, 320));
        if (splitPane != null) {
            splitPane.setDividerLocation(width);
        }
        revalidate();
        repaint();
    }

    private void refreshSectionButton(JToggleButton button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setSelected(selected);
        styleSectionButton(button, selected);
    }

    private void styleSectionButton(JToggleButton button, boolean selected) {
        Color panelBackground = UIUtil.getPanelBackground();
        boolean darkTheme = isDark(panelBackground);
        Color selectedBackground = darkTheme ? new Color(52, 68, 96) : new Color(226, 236, 251);
        Color selectedForeground = darkTheme ? Color.WHITE : new Color(41, 68, 122);
        Color idleForeground = UIUtil.getLabelForeground();
        Color idleBorder = mix(panelBackground, idleForeground, 0.10f);
        button.setBackground(selected ? selectedBackground : panelBackground);
        button.setForeground(selected ? selectedForeground : idleForeground);
        button.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(selected ? selectedBackground : idleBorder, 0, 0, 1, 0),
            JBUI.Borders.empty(7, 10)
        ));
    }

    private Component createSidebarItem(String title, String subtitle, String meta, boolean selected) {
        Color baseBackground = UIUtil.getPanelBackground();
        boolean darkTheme = isDark(baseBackground);
        Color selectedBackground = darkTheme ? new Color(57, 63, 79) : new Color(226, 236, 251);
        Color titleColor = selected ? contrastFor(selectedBackground) : UIUtil.getLabelForeground();
        Color subtitleColor = selected ? titleColor : UIUtil.getContextHelpForeground();

        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(true);
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(selected ? selectedBackground : mix(baseBackground, UIUtil.getLabelForeground(), 0.08f), 0, 0, 1, 0),
            JBUI.Borders.empty(6, 10)
        ));
        panel.setBackground(selected ? selectedBackground : baseBackground);

        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D()));
        titleLabel.setForeground(titleColor);
        titleLabel.setToolTipText(title);

        String detail = compactDetail(subtitle, meta);
        JBLabel detailLabel = new JBLabel(detail);
        detailLabel.setForeground(subtitleColor);
        detailLabel.setFont(detailLabel.getFont().deriveFont(Font.PLAIN, detailLabel.getFont().getSize2D() - 1f));
        detailLabel.setToolTipText(detail);

        JPanel center = new JPanel(new BorderLayout(0, 2));
        center.setOpaque(false);
        center.add(titleLabel, BorderLayout.NORTH);
        center.add(detailLabel, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private String compactDetail(String subtitle, String meta) {
        if (subtitle == null || subtitle.isBlank()) {
            return meta == null ? "" : meta;
        }
        if (meta == null || meta.isBlank()) {
            return subtitle;
        }
        return subtitle + " · " + meta;
    }

    private String shortenLocation(String location) {
        if (location == null || location.isBlank()) {
            return "暂无来源";
        }
        if (location.startsWith("http://") || location.startsWith("https://")) {
            try {
                URI uri = URI.create(location);
                String host = uri.getHost() == null ? location : uri.getHost();
                String path = uri.getPath() == null ? "" : uri.getPath();
                int index = path.lastIndexOf('/');
                String tail = index >= 0 && index + 1 < path.length() ? path.substring(index + 1) : path;
                return tail.isBlank() ? host : host + " · " + tail;
            } catch (IllegalArgumentException ignored) {
                return location;
            }
        }
        String normalized = location.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        if (index >= 0 && index + 1 < normalized.length()) {
            return normalized.substring(index + 1);
        }
        return normalized;
    }

    private Color mix(Color base, Color tint, float ratio) {
        int red = Math.round(base.getRed() * (1 - ratio) + tint.getRed() * ratio);
        int green = Math.round(base.getGreen() * (1 - ratio) + tint.getGreen() * ratio);
        int blue = Math.round(base.getBlue() * (1 - ratio) + tint.getBlue() * ratio);
        return new Color(red, green, blue);
    }

    private boolean isDark(Color color) {
        return (color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114) < 140;
    }

    private Color contrastFor(Color color) {
        return isDark(color) ? Color.WHITE : new Color(27, 39, 68);
    }

    private JScrollPane createListScrollPane(JList<?> list) {
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(UIUtil.getPanelBackground());
        scrollPane.getViewport().setBackground(UIUtil.getPanelBackground());
        return scrollPane;
    }

    private void configureSplitPane(JSplitPane splitPane) {
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(0);
        splitPane.setBackground(UIUtil.getPanelBackground());
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(java.awt.Graphics graphics) {
                        graphics.setColor(UIUtil.getPanelBackground());
                        graphics.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
                divider.setBorder(BorderFactory.createEmptyBorder());
                divider.setBackground(UIUtil.getPanelBackground());
                return divider;
            }
        });
    }

    private void installLibraryPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("从书架删除");
        deleteItem.addActionListener(this::deleteSelectedBook);
        popupMenu.add(deleteItem);

        libraryList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                maybeShowLibraryPopup(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                maybeShowLibraryPopup(event);
            }

            private void maybeShowLibraryPopup(MouseEvent event) {
                if (!event.isPopupTrigger()) {
                    return;
                }
                int index = libraryList.locationToIndex(event.getPoint());
                if (index < 0) {
                    return;
                }
                var bounds = libraryList.getCellBounds(index, index);
                if (bounds == null || !bounds.contains(event.getPoint())) {
                    return;
                }
                libraryPopupTarget = libraryModel.getElementAt(index);
                suppressLibraryOpen = true;
                try {
                    libraryList.setSelectedIndex(index);
                } finally {
                    suppressLibraryOpen = false;
                }
                popupMenu.show(libraryList, event.getX(), event.getY());
            }
        });
    }

    private void installBookmarkPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("删除书签");
        deleteItem.addActionListener(event -> deleteSelectedBookmark());
        popupMenu.add(deleteItem);

        bookmarkList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                maybeShowBookmarkPopup(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                maybeShowBookmarkPopup(event);
            }

            private void maybeShowBookmarkPopup(MouseEvent event) {
                if (!event.isPopupTrigger()) {
                    return;
                }
                int index = bookmarkList.locationToIndex(event.getPoint());
                if (index < 0) {
                    return;
                }
                var bounds = bookmarkList.getCellBounds(index, index);
                if (bounds == null || !bounds.contains(event.getPoint())) {
                    return;
                }
                bookmarkList.setSelectedIndex(index);
                popupMenu.show(bookmarkList, event.getX(), event.getY());
            }
        });
    }

    private void installBookmarkKeyboardDelete() {
        bookmarkList.registerKeyboardAction(
            event -> deleteSelectedBookmark(),
            KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            JComponent.WHEN_FOCUSED
        );
    }

    private void deleteSelectedBookmark() {
        Bookmark selected = bookmarkList.getSelectedValue();
        if (selected == null) {
            return;
        }
        boolean removed = stateService.removeBookmark(selected.getId());
        if (!removed) {
            Messages.showWarningDialog(project, "书签不存在或已经删除。", "FishNovel");
            return;
        }
        bookmarkList.clearSelection();
        refreshSidebar();
    }

    private void deleteSelectedBook(ActionEvent event) {
        BookShelfItem selected = libraryPopupTarget == null ? libraryList.getSelectedValue() : libraryPopupTarget;
        libraryPopupTarget = null;
        if (selected == null) {
            return;
        }
        boolean deletingCurrentBook = readerPanel.isShowingBook(selected.getBookId());
        stateService.removeBook(selected.getBookId());
        if (deletingCurrentBook) {
            readerPanel.clearBook();
        }
        libraryList.clearSelection();
        refreshSidebar();
    }

    private void importBook() {
        FileChooserDescriptor descriptor = SupportedBookFormats.createImportDescriptor("导入小说到 FishNovel");
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

    private void importWebBook() {
        String url = Messages.showInputDialog(
            project,
            "输入网页地址，例如：https://www.sudugu.org/5/20.html",
            "在线阅读网页小说",
            Messages.getQuestionIcon()
        );
        if (url == null || url.isBlank()) {
            return;
        }

        String targetUrl = url.trim();
        openRemoteUrlInPanel(targetUrl, "网页导入失败：\n");
    }

    private void openBookInPanel(BookShelfItem item) {
        if (item.getSourceType() == SourceType.REMOTE_URL) {
            openRemoteUrlInPanel(item.getSourceLocation(), "打开网页章节失败：\n");
            return;
        }
        try {
            readerPanel.openBook(projectService.reopen(item));
            refreshSidebar();
        } catch (IOException ex) {
            Messages.showErrorDialog(project, "打开小说失败：\n" + ex.getMessage(), "FishNovel");
        }
    }

    private void openRemoteUrlInPanel(String url, String errorPrefix) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "FishNovel loading web chapter", true) {
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    RemoteChapterLoadResult result = projectService.importRemoteChapterFromUrl(url);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        refreshSidebar();
                        readerPanel.openRemoteChapter(result);
                    });
                } catch (IOException ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showErrorDialog(project, errorPrefix + ex.getMessage(), "FishNovel")
                    );
                }
            }
        });
    }

    private void openBookmarkInPanel(Bookmark bookmark) {
        stateService.findBook(bookmark.getBookId()).ifPresent(item -> {
            try {
                BookDocument document = projectService.reopen(item);
                readerPanel.openBook(
                    document,
                    new ReadingProgress(
                        bookmark.getChapterIndex(),
                        bookmark.getContentOffset(),
                        System.currentTimeMillis(),
                        bookmark.getChapterTitle()
                    )
                );
                refreshSidebar();
            } catch (IOException ex) {
                Messages.showErrorDialog(project, "跳转书签失败：\n" + ex.getMessage(), "FishNovel");
            }
        });
    }

    private void refreshSidebar() {
        refillModel(libraryModel, stateService.listBooks());
        refillModel(recentModel, stateService.listRecentEntries());
        refillModel(bookmarkModel, stateService.listBookmarks());
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
