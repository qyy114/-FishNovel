package com.fishnovel.idea.ui;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.BookShelfItem;
import com.fishnovel.idea.model.Bookmark;
import com.fishnovel.idea.model.ReadingProgress;
import com.fishnovel.idea.model.RecentEntry;
import com.fishnovel.idea.service.FishNovelProjectService;
import com.fishnovel.idea.service.ReadingStateService;
import com.fishnovel.idea.util.SupportedBookFormats;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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

public final class FishNovelToolWindowPanel extends JPanel {
    private static final String SECTION_LIBRARY = "library";
    private static final String SECTION_RECENT = "recent";
    private static final String SECTION_BOOKMARKS = "bookmarks";

    private final Project project;
    private final FishNovelProjectService projectService;
    private final ReadingStateService stateService;
    private final BookReaderPanel readerPanel;
    private final DefaultListModel<BookShelfItem> libraryModel = new DefaultListModel<>();
    private final DefaultListModel<RecentEntry> recentModel = new DefaultListModel<>();
    private final DefaultListModel<Bookmark> bookmarkModel = new DefaultListModel<>();
    private final JBList<BookShelfItem> libraryList = new JBList<>(libraryModel);
    private final JBList<RecentEntry> recentList = new JBList<>(recentModel);
    private final JBList<Bookmark> bookmarkList = new JBList<>(bookmarkModel);
    private final CardLayout sectionCards = new CardLayout();
    private final JPanel sectionContent = new JPanel(sectionCards);

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
        setBackground(UIUtil.getPanelBackground());

        JButton importButton = createToolbarButton("导入小说", true);
        JButton onlineReadButton = createToolbarButton("在线阅读", false);
        importButton.addActionListener(event -> importBook());
        onlineReadButton.addActionListener(event -> importWebBook());

        JPanel toolbar = new JPanel();
        toolbar.setOpaque(false);
        toolbar.add(importButton);
        toolbar.add(onlineReadButton);

        configureList(libraryList, "暂无书架");
        configureList(recentList, "暂无最近阅读");
        configureList(bookmarkList, "暂无书签");
        installLibraryPopupMenu();
        installBookmarkPopupMenu();
        installBookmarkKeyboardDelete();

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

        libraryList.setCellRenderer((list, value, index, isSelected, cellHasFocus) ->
            createSidebarItem(value.getTitle(), shortenLocation(value.getSourceLocation()), value.getBookmarkCount() + " 个书签", isSelected)
        );
        recentList.setCellRenderer((list, value, index, isSelected, cellHasFocus) ->
            createSidebarItem(value.getTitle(), shortenLocation(value.getSourceLocation()), "最近阅读", isSelected)
        );
        bookmarkList.setCellRenderer((list, value, index, isSelected, cellHasFocus) ->
            createSidebarItem(value.getBookTitle(), value.getChapterTitle(), "书签定位", isSelected)
        );

        sectionContent.setOpaque(false);
        sectionContent.add(createListScrollPane(libraryList), SECTION_LIBRARY);
        sectionContent.add(createListScrollPane(recentList), SECTION_RECENT);
        sectionContent.add(createListScrollPane(bookmarkList), SECTION_BOOKMARKS);

        JPanel navigationPanel = createNavigationRail();

        JPanel leftPane = new JPanel(new BorderLayout(12, 0));
        leftPane.setOpaque(false);
        leftPane.add(navigationPanel, BorderLayout.WEST);
        leftPane.add(sectionContent, BorderLayout.CENTER);
        leftPane.setPreferredSize(new Dimension(360, 520));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, readerPanel);
        splitPane.setResizeWeight(0.34);
        configureSplitPane(splitPane);

        add(toolbar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createNavigationRail() {
        JPanel navigationPanel = new JPanel();
        navigationPanel.setOpaque(false);
        navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.Y_AXIS));
        navigationPanel.setBorder(JBUI.Borders.empty(8, 0, 8, 6));

        JBLabel brandLabel = new JBLabel("FishNovel");
        brandLabel.setFont(brandLabel.getFont().deriveFont(Font.BOLD, 16f));
        JBLabel subLabel = new JBLabel("摸鱼阅读");
        subLabel.setForeground(UIUtil.getContextHelpForeground());
        subLabel.setFont(subLabel.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel brandPanel = new JPanel();
        brandPanel.setOpaque(false);
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBorder(JBUI.Borders.empty(0, 8, 12, 0));
        brandPanel.add(brandLabel);
        brandPanel.add(Box.createVerticalStrut(2));
        brandPanel.add(subLabel);

        ButtonGroup buttonGroup = new ButtonGroup();
        navigationPanel.add(brandPanel);
        navigationPanel.add(createSectionButton("书架", SECTION_LIBRARY, buttonGroup, true));
        navigationPanel.add(Box.createVerticalStrut(8));
        navigationPanel.add(createSectionButton("最近", SECTION_RECENT, buttonGroup, false));
        navigationPanel.add(Box.createVerticalStrut(8));
        navigationPanel.add(createSectionButton("书签", SECTION_BOOKMARKS, buttonGroup, false));
        navigationPanel.add(Box.createVerticalGlue());
        return navigationPanel;
    }

    private JButton createToolbarButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 12f));
        button.setBorder(JBUI.Borders.empty(8, 14));
        button.setOpaque(true);

        Color panelBackground = UIUtil.getPanelBackground();
        Color labelColor = UIUtil.getLabelForeground();
        Color accent = UIUtil.getListSelectionBackground(true);
        if (accent == null) {
            accent = isDark(panelBackground) ? new Color(72, 103, 187) : new Color(219, 231, 255);
        }

        if (primary) {
            button.setBackground(accent);
            button.setForeground(contrastFor(accent));
        } else {
            button.setBackground(mix(panelBackground, labelColor, 0.06f));
            button.setForeground(labelColor);
        }
        return button;
    }

    private void configureList(JBList<?> list, String emptyText) {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(76);
        list.setBorder(BorderFactory.createEmptyBorder());
        list.setBackground(UIUtil.getPanelBackground());
        list.getEmptyText().setText(emptyText);
    }

    private JToggleButton createSectionButton(String text, String sectionKey, ButtonGroup group, boolean selected) {
        JToggleButton button = new JToggleButton("<html><div style='text-align:center;line-height:1.35'>" + text + "</div></html>");
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setFocusPainted(false);
        button.setBorder(JBUI.Borders.empty(14, 18));
        button.setPreferredSize(new Dimension(82, 72));
        button.setMaximumSize(new Dimension(82, 72));
        button.setOpaque(true);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        styleSectionButton(button, selected);
        button.addActionListener(event -> {
            sectionCards.show(sectionContent, sectionKey);
            refreshNavigationStyles(group);
        });
        group.add(button);
        button.setSelected(selected);
        if (selected) {
            sectionCards.show(sectionContent, sectionKey);
        }
        return button;
    }

    private void refreshNavigationStyles(ButtonGroup group) {
        var elements = group.getElements();
        while (elements.hasMoreElements()) {
            JToggleButton button = (JToggleButton) elements.nextElement();
            styleSectionButton(button, button.isSelected());
        }
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
            JBUI.Borders.customLine(selected ? selectedBackground : idleBorder, 1),
            JBUI.Borders.empty(14, 18)
        ));
    }

    private Component createSidebarItem(String title, String subtitle, String meta, boolean selected) {
        Color baseBackground = UIUtil.getPanelBackground();
        boolean darkTheme = isDark(baseBackground);
        Color selectedBackground = darkTheme ? new Color(57, 63, 79) : new Color(226, 236, 251);
        Color titleColor = selected ? contrastFor(selectedBackground) : UIUtil.getLabelForeground();
        Color subtitleColor = selected ? titleColor : UIUtil.getContextHelpForeground();

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(true);
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(selected ? selectedBackground : mix(baseBackground, UIUtil.getLabelForeground(), 0.08f), 0, 0, 1, 0),
            JBUI.Borders.empty(8, 10)
        ));
        panel.setBackground(selected ? selectedBackground : baseBackground);

        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D()));
        titleLabel.setForeground(titleColor);

        JBLabel subtitleLabel = new JBLabel(subtitle);
        subtitleLabel.setForeground(subtitleColor);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, subtitleLabel.getFont().getSize2D() - 1f));

        JBLabel metaLabel = new JBLabel(meta);
        metaLabel.setForeground(selected ? titleColor : mix(UIUtil.getLabelForeground(), baseBackground, 0.45f));
        metaLabel.setFont(metaLabel.getFont().deriveFont(Font.PLAIN, metaLabel.getFont().getSize2D() - 2f));

        JPanel center = new JPanel(new BorderLayout(0, 2));
        center.setOpaque(false);
        center.add(titleLabel, BorderLayout.NORTH);
        center.add(subtitleLabel, BorderLayout.CENTER);
        center.add(metaLabel, BorderLayout.SOUTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
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
        splitPane.setDividerSize(2);
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
                libraryList.setSelectedIndex(index);
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
        BookShelfItem selected = libraryList.getSelectedValue();
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

        try {
            BookDocument document = projectService.importBookFromUrl(url.trim());
            refreshSidebar();
            readerPanel.openBook(document);
        } catch (IOException ex) {
            Messages.showErrorDialog(project, "网页导入失败：\n" + ex.getMessage(), "FishNovel");
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
