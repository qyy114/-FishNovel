package com.fishnovel.idea.ui;

import static com.fishnovel.idea.FishNovelBundle.message;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Bookmark;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.ReaderTheme;
import com.fishnovel.idea.model.ReadingPreferences;
import com.fishnovel.idea.model.ReadingProgress;
import com.fishnovel.idea.model.SourceType;
import com.fishnovel.idea.service.ChapterJumpResolver;
import com.fishnovel.idea.service.FishNovelProjectService;
import com.fishnovel.idea.service.ReadingProgressResolver;
import com.fishnovel.idea.service.ReadingStateService;
import com.fishnovel.idea.source.RemoteChapterLoadResult;
import com.fishnovel.idea.source.RemoteChapterNavigation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.OptionalInt;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public final class BookReaderPanel extends JPanel {
    private static final int MIN_FONT_SIZE = 13;
    private static final int MAX_FONT_SIZE = 30;

    private final Project project;
    private final FishNovelProjectService projectService;
    private final ReadingStateService stateService;
    private final Runnable onStateChanged;

    private final JBLabel chapterMetaLabel = new JBLabel(message("reader.empty"));
    private final JComboBox<Chapter> chapterSelector = new JComboBox<>();
    private final JComboBox<ReaderTheme> themeSelector = new JComboBox<>(ReaderTheme.values());
    private final JTextPane textPane = new JTextPane();
    private final JScrollPane scrollPane = new JScrollPane(textPane);
    private final JPanel readerShell = new JPanel(new GridBagLayout());
    private final JPanel readerCard = new JPanel(new BorderLayout());
    private final JPanel topPanel = new JPanel(new BorderLayout(0, 3));
    private final JPanel controlPanel = new JPanel(new WrappingFlowLayout(FlowLayout.LEFT, 4, 4));
    private final JPanel bottomNavigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
    private final JButton bookmarkButton = new JButton(message("reader.button.addBookmark"));
    private final JButton refreshButton = new JButton(message("reader.button.refresh"));
    private final JButton jumpButton = new JButton(message("reader.button.jump"));
    private final JButton previousChapterButton = new JButton(message("reader.button.previous"));
    private final JButton nextChapterButton = new JButton(message("reader.button.next"));
    private final JButton fontMinusButton = new JButton("A-");
    private final JButton fontPlusButton = new JButton("A+");
    private final JButton spacingMinusButton = new JButton(message("reader.button.spacingCompact"));
    private final JButton spacingPlusButton = new JButton(message("reader.button.spacingRelaxed"));

    private BookDocument currentDocument;
    private RemoteChapterNavigation remoteNavigation;
    private boolean adjusting;
    private boolean loading;
    private boolean controlsCollapsed = false;

    public BookReaderPanel(Project project, Runnable onStateChanged) {
        super(new BorderLayout());
        this.project = project;
        this.projectService = FishNovelProjectService.getInstance(project);
        this.stateService = ReadingStateService.getInstance();
        this.onStateChanged = onStateChanged;

        buildUi();
        registerListeners();
        clearBook();
    }

    private void buildUi() {
        setBorder(JBUI.Borders.empty());

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBorder(JBUI.Borders.emptyBottom(3));
        header.setOpaque(false);

        chapterMetaLabel.setFont(chapterMetaLabel.getFont().deriveFont(Font.PLAIN, 12f));
        header.add(chapterMetaLabel, BorderLayout.WEST);

        controlPanel.setOpaque(false);
        controlPanel.add(new JBLabel(message("reader.label.chapter")));
        chapterSelector.setPreferredSize(new Dimension(168, 28));
        controlPanel.add(chapterSelector);
        controlPanel.add(jumpButton);
        controlPanel.add(refreshButton);
        previousChapterButton.setToolTipText(message("reader.button.previous"));
        nextChapterButton.setToolTipText(message("reader.button.next"));
        controlPanel.add(fontMinusButton);
        controlPanel.add(fontPlusButton);
        controlPanel.add(spacingMinusButton);
        controlPanel.add(spacingPlusButton);
        controlPanel.add(new JBLabel(message("reader.label.theme")));
        controlPanel.add(themeSelector);
        controlPanel.add(bookmarkButton);

        topPanel.setOpaque(false);
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(controlPanel, BorderLayout.SOUTH);
        updateControlsCollapsedState();

        textPane.setEditable(false);
        textPane.setBorder(new EmptyBorder(10, 8, 12, 10));
        textPane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        textPane.setOpaque(true);

        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(28);
        scrollPane.setOpaque(true);

        readerCard.add(scrollPane, BorderLayout.CENTER);
        bottomNavigationPanel.setOpaque(false);
        bottomNavigationPanel.add(previousChapterButton);
        bottomNavigationPanel.add(nextChapterButton);
        readerCard.add(bottomNavigationPanel, BorderLayout.SOUTH);
        readerCard.setMinimumSize(new Dimension(0, 0));
        readerCard.setBorder(BorderFactory.createEmptyBorder());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        readerShell.add(readerCard, constraints);
        readerShell.setMinimumSize(new Dimension(0, 0));
        readerShell.setOpaque(true);

        add(topPanel, BorderLayout.NORTH);
        add(readerShell, BorderLayout.CENTER);
    }

    private void registerListeners() {
        chapterSelector.addActionListener(event -> {
            if (!adjusting && currentDocument != null && chapterSelector.getSelectedIndex() >= 0) {
                renderChapter(chapterSelector.getSelectedIndex(), 0);
            }
        });

        themeSelector.addActionListener(event -> {
            if (!adjusting && currentDocument != null) {
                updatePreferences(
                    getCurrentPreferences().getFontSize(),
                    getCurrentPreferences().getLineSpacing(),
                    (ReaderTheme) themeSelector.getSelectedItem()
                );
            }
        });

        fontMinusButton.addActionListener(event -> shiftFontSize(-1));
        fontPlusButton.addActionListener(event -> shiftFontSize(1));
        spacingMinusButton.addActionListener(event -> shiftLineSpacing(-0.06f));
        spacingPlusButton.addActionListener(event -> shiftLineSpacing(0.06f));
        bookmarkButton.addActionListener(event -> addBookmark());
        refreshButton.addActionListener(event -> refreshCurrentBook());
        jumpButton.addActionListener(event -> jumpToChapter());
        previousChapterButton.addActionListener(event -> navigateChapter(-1));
        nextChapterButton.addActionListener(event -> navigateChapter(1));

        scrollPane.getVerticalScrollBar().addAdjustmentListener(event -> {
            if (!event.getValueIsAdjusting() && currentDocument != null && !adjusting) {
                SwingUtilities.invokeLater(this::persistCurrentPosition);
            }
        });
    }

    public void openBook(BookDocument document) {
        openBook(document, null);
    }

    public void openBook(BookDocument document, ReadingProgress overrideProgress) {
        openBook(document, overrideProgress, null);
    }

    public void openRemoteChapter(RemoteChapterLoadResult result) {
        openRemoteChapter(result, null);
    }

    public void openRemoteChapter(RemoteChapterLoadResult result, ReadingProgress overrideProgress) {
        openBook(result.getDocument(), overrideProgress, result.getNavigation());
        if (result.hasWarning()) {
            Messages.showWarningDialog(project, result.getWarning(), message("plugin.name"));
        }
    }

    public void setControlsCollapsed(boolean collapsed) {
        controlsCollapsed = collapsed;
        updateControlsCollapsedState();
    }

    public JBLabel getChapterMetaLabel() {
        return chapterMetaLabel;
    }

    public void refreshChromeLayout() {
        controlPanel.invalidate();
        topPanel.invalidate();
        revalidate();
        repaint();
    }

    public void refreshCurrentBook() {
        if (currentDocument == null) {
            Messages.showWarningDialog(project, message("reader.warning.noBook"), message("plugin.name"));
            return;
        }
        if (loading) {
            return;
        }

        BookDocument document = currentDocument;
        ReadingProgress progress = currentProgressSnapshot();
        if (document.getSourceType() == SourceType.TOMATO_TXT) {
            refreshTomatoDocument(document, progress);
            return;
        }
        if (document.getSourceType() == SourceType.REMOTE_URL) {
            refreshRemoteDocument(document, progress);
            return;
        }
        refreshLocalDocument(document, progress);
    }

    private void openBook(BookDocument document, ReadingProgress overrideProgress, RemoteChapterNavigation navigation) {
        currentDocument = document;
        remoteNavigation = navigation;
        adjusting = true;
        stateService.registerBook(document);

        chapterSelector.removeAllItems();
        for (Chapter chapter : document.getChapters()) {
            chapterSelector.addItem(chapter);
        }

        ReadingPreferences preferences = stateService.getPreferences(document.getBookId());
        themeSelector.setSelectedItem(preferences.getTheme());
        applyPreferences(preferences);

        ReadingProgress progress = overrideProgress == null ? stateService.getProgress(document.getBookId()) : overrideProgress;
        int chapterIndex = ReadingProgressResolver.resolveChapterIndex(document, progress);
        int contentOffset = ReadingProgressResolver.resolveContentOffset(document, progress, chapterIndex);
        chapterSelector.setSelectedIndex(chapterIndex);
        adjusting = false;
        renderChapter(chapterIndex, contentOffset);
    }

    public void clearBook() {
        currentDocument = null;
        remoteNavigation = null;
        adjusting = true;
        chapterMetaLabel.setText(message("reader.empty"));
        chapterSelector.removeAllItems();
        textPane.setText("");
        applyPreferences(ReadingPreferences.defaults());
        updateChapterNavigationButtons();
        adjusting = false;
    }

    public boolean isShowingBook(String bookId) {
        return currentDocument != null && currentDocument.getBookId().equals(bookId);
    }

    private void renderChapter(int chapterIndex, int contentOffset) {
        if (currentDocument == null) {
            return;
        }

        Chapter chapter = currentDocument.getChapter(chapterIndex);
        adjusting = true;
        chapterMetaLabel.setText(message("reader.chapterMeta", chapterIndex + 1, currentDocument.getChapters().size(), chapter.getTitle()));
        StyledDocument styledDocument = textPane.getStyledDocument();
        try {
            styledDocument.remove(0, styledDocument.getLength());
            styledDocument.insertString(0, chapter.getContent(), null);
            applyParagraphStyle(getCurrentPreferences());
        } catch (BadLocationException ex) {
            Messages.showErrorDialog(project, message("reader.error.renderPrefix") + ex.getMessage(), message("plugin.name"));
        }

        restoreOffset(contentOffset);
        adjusting = false;
        updateChapterNavigationButtons();
        persistProgress(chapterIndex, contentOffset);
    }

    private void navigateChapter(int delta) {
        if (currentDocument == null || loading) {
            return;
        }
        if (currentDocument.getSourceType() == SourceType.REMOTE_URL) {
            String targetUrl = delta < 0
                ? (remoteNavigation == null ? null : remoteNavigation.getPreviousUrl())
                : (remoteNavigation == null ? null : remoteNavigation.getNextUrl());
            loadRemoteChapter(targetUrl);
            return;
        }

        int currentIndex = chapterSelector.getSelectedIndex();
        int targetIndex = currentIndex + delta;
        if (targetIndex < 0 || targetIndex >= currentDocument.getChapters().size()) {
            return;
        }
        adjusting = true;
        chapterSelector.setSelectedIndex(targetIndex);
        adjusting = false;
        renderChapter(targetIndex, 0);
    }

    private void jumpToChapter() {
        if (currentDocument == null) {
            return;
        }
        String input = Messages.showInputDialog(
            project,
            message("reader.dialog.jump.prompt"),
            message("reader.dialog.jump.title"),
            Messages.getQuestionIcon()
        );
        if (input == null) {
            return;
        }
        OptionalInt chapterNumber = ChapterJumpResolver.parsePositiveNumber(input);
        if (chapterNumber.isEmpty()) {
            Messages.showWarningDialog(project, message("reader.warning.invalidChapterNumber"), message("plugin.name"));
            return;
        }
        if (currentDocument.getSourceType() == SourceType.REMOTE_URL) {
            loadRemoteChapterByNumber(chapterNumber.getAsInt());
            return;
        }
        OptionalInt chapterIndex = ChapterJumpResolver.resolveLocalChapterIndex(currentDocument, input);
        if (chapterIndex.isEmpty()) {
            Messages.showWarningDialog(project, message("reader.warning.chapterNotFound", chapterNumber.getAsInt()), message("plugin.name"));
            return;
        }
        adjusting = true;
        chapterSelector.setSelectedIndex(chapterIndex.getAsInt());
        adjusting = false;
        renderChapter(chapterIndex.getAsInt(), 0);
    }

    private void loadRemoteChapter(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (loading) {
            return;
        }
        setReaderLoading(true);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, message("reader.task.loadingWebChapter"), true) {
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    RemoteChapterLoadResult result = projectService.importRemoteChapterFromUrl(url);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setReaderLoading(false);
                        openRemoteChapter(result);
                    });
                } catch (IOException ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setReaderLoading(false);
                        Messages.showErrorDialog(project, message("reader.error.webLoadPrefix") + ex.getMessage(), message("plugin.name"));
                    });
                }
            }
        });
    }

    private void loadRemoteChapterByNumber(int chapterNumber) {
        String currentUrl = remoteNavigation == null ? currentDocument.getSourceLocation() : remoteNavigation.getCurrentUrl();
        if (currentUrl == null || currentUrl.isBlank()) {
            Messages.showWarningDialog(project, message("reader.warning.remoteJumpUnsupported"), message("plugin.name"));
            return;
        }
        if (loading) {
            return;
        }
        setReaderLoading(true);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, message("reader.task.jumpingWebChapter"), true) {
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    RemoteChapterLoadResult result = projectService.importRemoteChapterByNumber(currentUrl, chapterNumber);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setReaderLoading(false);
                        openRemoteChapter(result);
                    });
                } catch (IOException ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setReaderLoading(false);
                        Messages.showErrorDialog(project, message("reader.error.webJumpPrefix") + ex.getMessage(), message("plugin.name"));
                    });
                }
            }
        });
    }

    private void refreshTomatoDocument(BookDocument document, ReadingProgress progress) {
        setReaderLoading(true);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, message("reader.task.refreshingTomatoBook"), true) {
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    BookDocument refreshed = projectService.refreshTomatoBook(document);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setReaderLoading(false);
                        openBook(refreshed, progress);
                    });
                } catch (IOException ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setReaderLoading(false);
                        Messages.showErrorDialog(project, message("reader.error.refreshTomatoPrefix") + ex.getMessage(), message("plugin.name"));
                    });
                }
            }
        });
    }

    private void refreshLocalDocument(BookDocument document, ReadingProgress progress) {
        Path sourcePath;
        try {
            sourcePath = resolveLocalSourcePath(document);
        } catch (IOException ex) {
            Messages.showErrorDialog(project, message("reader.error.refreshBookPrefix") + ex.getMessage(), message("plugin.name"));
            return;
        }

        setReaderLoading(true);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, message("reader.task.refreshingBook"), true) {
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    BookDocument refreshed = projectService.importBook(sourcePath);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setReaderLoading(false);
                        openBook(refreshed, progress);
                    });
                } catch (IOException ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setReaderLoading(false);
                        Messages.showErrorDialog(project, message("reader.error.refreshBookPrefix") + ex.getMessage(), message("plugin.name"));
                    });
                }
            }
        });
    }

    private void refreshRemoteDocument(BookDocument document, ReadingProgress progress) {
        String currentUrl = remoteNavigation == null ? document.getSourceLocation() : remoteNavigation.getCurrentUrl();
        if (currentUrl == null || currentUrl.isBlank()) {
            Messages.showWarningDialog(project, message("reader.warning.noRefreshUrl"), message("plugin.name"));
            return;
        }

        setReaderLoading(true);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, message("reader.task.refreshingWebChapter"), true) {
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    RemoteChapterLoadResult result = projectService.importRemoteChapterFromUrl(currentUrl);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setReaderLoading(false);
                        openBook(result.getDocument(), progress, result.getNavigation());
                        if (result.hasWarning()) {
                            Messages.showWarningDialog(project, result.getWarning(), message("plugin.name"));
                        }
                    });
                } catch (IOException ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setReaderLoading(false);
                        Messages.showErrorDialog(project, message("reader.error.refreshWebPrefix") + ex.getMessage(), message("plugin.name"));
                    });
                }
            }
        });
    }

    private Path resolveLocalSourcePath(BookDocument document) throws IOException {
        if (document.getSourcePath() != null) {
            return document.getSourcePath();
        }
        String sourceLocation = document.getSourceLocation();
        if (sourceLocation == null || sourceLocation.isBlank()) {
            throw new IOException(message("reader.error.noLocalPath"));
        }
        try {
            return Path.of(sourceLocation);
        } catch (InvalidPathException ex) {
            throw new IOException(message("reader.error.invalidLocalPath", sourceLocation), ex);
        }
    }

    private ReadingProgress currentProgressSnapshot() {
        int chapterIndex = Math.max(0, chapterSelector.getSelectedIndex());
        if (currentDocument.getChapters().isEmpty()) {
            return ReadingProgress.defaults();
        }
        String chapterKey = currentDocument.getChapter(chapterIndex).getTitle();
        return new ReadingProgress(chapterIndex, currentOffset(), System.currentTimeMillis(), chapterKey);
    }

    private void setReaderLoading(boolean loading) {
        this.loading = loading;
        updateChapterNavigationButtons();
    }

    private void updateControlsCollapsedState() {
        topPanel.setVisible(!controlsCollapsed);
        controlPanel.setVisible(!controlsCollapsed);
        revalidate();
        repaint();
    }

    private void updateChapterNavigationButtons() {
        boolean hasDocument = currentDocument != null;
        boolean remoteDocument = hasDocument
            && currentDocument.getSourceType() == SourceType.REMOTE_URL
            && remoteNavigation != null;
        int selectedIndex = chapterSelector.getSelectedIndex();
        int chapterCount = hasDocument ? currentDocument.getChapters().size() : 0;
        boolean hasPrevious = remoteDocument ? remoteNavigation.hasPrevious() : selectedIndex > 0;
        boolean hasNext = remoteDocument ? remoteNavigation.hasNext() : selectedIndex >= 0 && selectedIndex < chapterCount - 1;

        jumpButton.setEnabled(hasDocument && !loading);
        refreshButton.setEnabled(hasDocument && !loading);
        bottomNavigationPanel.setVisible(hasDocument);
        previousChapterButton.setEnabled(hasDocument && hasPrevious && !loading);
        nextChapterButton.setEnabled(hasDocument && hasNext && !loading);
        if (bottomNavigationPanel.getParent() != null) {
            bottomNavigationPanel.getParent().revalidate();
            bottomNavigationPanel.getParent().repaint();
        }
    }

    private void restoreOffset(int contentOffset) {
        SwingUtilities.invokeLater(() -> {
            try {
                int safeOffset = Math.max(0, Math.min(contentOffset, textPane.getDocument().getLength()));
                Rectangle2D rectangle = textPane.modelToView2D(safeOffset);
                JViewport viewport = scrollPane.getViewport();
                if (rectangle != null) {
                    viewport.setViewPosition(new Point(0, Math.max(0, (int) rectangle.getY() - 36)));
                } else {
                    viewport.setViewPosition(new Point(0, 0));
                }
                textPane.setCaretPosition(safeOffset);
            } catch (BadLocationException ignored) {
                scrollPane.getViewport().setViewPosition(new Point(0, 0));
            }
        });
    }

    private void persistCurrentPosition() {
        if (currentDocument == null) {
            return;
        }
        persistProgress(chapterSelector.getSelectedIndex(), currentOffset());
    }

    private void persistProgress(int chapterIndex, int contentOffset) {
        if (currentDocument == null) {
            return;
        }
        stateService.updateProgress(
            currentDocument.getBookId(),
            new ReadingProgress(
                Math.max(0, chapterIndex),
                Math.max(0, contentOffset),
                System.currentTimeMillis(),
                currentDocument.getChapter(chapterIndex).getTitle()
            )
        );
        onStateChanged.run();
    }

    private void addBookmark() {
        if (currentDocument == null) {
            return;
        }
        Chapter chapter = currentDocument.getChapter(chapterSelector.getSelectedIndex());
        Bookmark bookmark = stateService.addBookmark(
            currentDocument.getBookId(),
            currentDocument.getTitle(),
            chapter.getTitle(),
            chapterSelector.getSelectedIndex(),
            currentOffset()
        );
        onStateChanged.run();
        Messages.showInfoMessage(project, message("reader.info.bookmarkAdded", bookmark.getChapterTitle()), message("plugin.name"));
    }

    private int currentOffset() {
        Point point = scrollPane.getViewport().getViewPosition();
        return Math.max(0, textPane.viewToModel2D(point));
    }

    private void shiftFontSize(int delta) {
        ReadingPreferences preferences = getCurrentPreferences();
        int currentSize = effectiveFontSize(preferences, resolveSystemFont());
        updatePreferences(
            Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, currentSize + delta)),
            preferences.getLineSpacing(),
            preferences.getTheme()
        );
    }

    private void shiftLineSpacing(float delta) {
        ReadingPreferences preferences = getCurrentPreferences();
        float next = Math.max(0.12f, Math.min(0.9f, preferences.getLineSpacing() + delta));
        updatePreferences(preferences.getFontSize(), next, preferences.getTheme());
    }

    private void updatePreferences(int fontSize, float lineSpacing, ReaderTheme theme) {
        if (currentDocument == null) {
            return;
        }
        ReadingPreferences updated = new ReadingPreferences(fontSize, lineSpacing, theme);
        stateService.updatePreferences(currentDocument.getBookId(), updated);
        applyPreferences(updated);
        applyParagraphStyle(updated);
        onStateChanged.run();
    }

    private ReadingPreferences getCurrentPreferences() {
        if (currentDocument == null) {
            return ReadingPreferences.defaults();
        }
        return stateService.getPreferences(currentDocument.getBookId());
    }

    private void applyPreferences(ReadingPreferences preferences) {
        ReaderPalette palette = resolvePalette(preferences.getTheme());
        themeSelector.setSelectedItem(preferences.getTheme());

        Font uiFont = resolveSystemFont();
        int fontSize = effectiveFontSize(preferences, uiFont);
        textPane.setFont(uiFont.deriveFont(uiFont.getStyle(), (float) fontSize));

        setBackground(palette.shellBackground);
        setOpaque(true);
        readerShell.setBackground(palette.shellBackground);
        readerShell.setOpaque(true);
        readerCard.setBackground(palette.cardBackground);
        readerCard.setOpaque(true);
        readerCard.setBorder(BorderFactory.createEmptyBorder());

        chapterMetaLabel.setForeground(palette.metaColor);

        scrollPane.setBackground(palette.cardBackground);
        scrollPane.getViewport().setBackground(palette.cardBackground);
        textPane.setBackground(palette.cardBackground);
        textPane.setForeground(palette.textColor);
        textPane.setCaretColor(palette.textColor);
        textPane.setSelectionColor(palette.selectionColor);
        textPane.setSelectedTextColor(palette.textColor);

        styleButton(bookmarkButton, palette, true);
        styleButton(refreshButton, palette, false);
        styleButton(jumpButton, palette, false);
        styleButton(previousChapterButton, palette, false);
        styleButton(nextChapterButton, palette, false);
        styleButton(fontMinusButton, palette, false);
        styleButton(fontPlusButton, palette, false);
        styleButton(spacingMinusButton, palette, false);
        styleButton(spacingPlusButton, palette, false);

        chapterSelector.setBackground(palette.cardBackground);
        chapterSelector.setForeground(palette.textColor);
        chapterSelector.setFont(uiFont);
        themeSelector.setBackground(palette.cardBackground);
        themeSelector.setForeground(palette.textColor);
        themeSelector.setFont(uiFont);
        repaint();
    }

    private Font resolveSystemFont() {
        Font font = UIManager.getFont("TextPane.font");
        if (font == null) {
            font = UIManager.getFont("EditorPane.font");
        }
        if (font == null) {
            font = UIManager.getFont("Label.font");
        }
        return font == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 14) : font;
    }

    private int effectiveFontSize(ReadingPreferences preferences, Font uiFont) {
        return preferences.getFontSize() > 0 ? preferences.getFontSize() : uiFont.getSize();
    }

    private void applyParagraphStyle(ReadingPreferences preferences) {
        StyledDocument document = textPane.getStyledDocument();
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        int fontSize = effectiveFontSize(preferences, textPane.getFont());
        StyleConstants.setFontFamily(attributes, textPane.getFont().getFamily());
        StyleConstants.setFontSize(attributes, fontSize);
        StyleConstants.setLineSpacing(attributes, preferences.getLineSpacing());
        StyleConstants.setFirstLineIndent(attributes, 0f);
        document.setParagraphAttributes(0, document.getLength(), attributes, false);
    }

    private void styleButton(JButton button, ReaderPalette palette, boolean primary) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        Font systemFont = resolveSystemFont();
        button.setFont(systemFont.deriveFont(Font.PLAIN, systemFont.getSize2D()));
        button.setBorder(JBUI.Borders.empty(5, 8));
        if (primary) {
            button.setBackground(palette.actionBackground);
            button.setForeground(palette.actionForeground);
        } else {
            button.setBackground(palette.secondaryActionBackground);
            button.setForeground(palette.secondaryActionForeground);
        }
    }

    private ReaderPalette resolvePalette(ReaderTheme theme) {
        if (theme == ReaderTheme.AUTO) {
            return ReaderPalette.auto();
        }
        return switch (theme) {
            case PAPER -> ReaderPalette.paper();
            case DARK -> ReaderPalette.manualDark();
            case FOREST -> ReaderPalette.forest();
            case AUTO -> throw new IllegalStateException("AUTO should have been handled earlier");
        };
    }

    public JComponent getPreferredFocusedComponent() {
        return textPane;
    }

    public void disposePanel() {
    }

    private static final class ReaderPalette {
        private final Color shellBackground;
        private final Color cardBackground;
        private final Color titleColor;
        private final Color metaColor;
        private final Color subtleMetaColor;
        private final Color textColor;
        private final Color selectionColor;
        private final Color actionBackground;
        private final Color actionForeground;
        private final Color secondaryActionBackground;
        private final Color secondaryActionForeground;

        private ReaderPalette(
            Color shellBackground,
            Color cardBackground,
            Color titleColor,
            Color metaColor,
            Color subtleMetaColor,
            Color textColor,
            Color selectionColor,
            Color actionBackground,
            Color actionForeground,
            Color secondaryActionBackground,
            Color secondaryActionForeground
        ) {
            this.shellBackground = shellBackground;
            this.cardBackground = cardBackground;
            this.titleColor = titleColor;
            this.metaColor = metaColor;
            this.subtleMetaColor = subtleMetaColor;
            this.textColor = textColor;
            this.selectionColor = selectionColor;
            this.actionBackground = actionBackground;
            this.actionForeground = actionForeground;
            this.secondaryActionBackground = secondaryActionBackground;
            this.secondaryActionForeground = secondaryActionForeground;
        }

        private static ReaderPalette auto() {
            Color shell = safeColor(UIUtil.getPanelBackground(), new Color(242, 242, 242));
            Color label = safeColor(UIUtil.getLabelForeground(), new Color(60, 60, 60));
            Color meta = blend(label, shell, 0.62f);
            Color subtle = blend(label, shell, 0.42f);
            Color buttonBackground = safeColor(UIManager.getColor("Button.background"), shell);
            Color buttonForeground = safeColor(UIManager.getColor("Button.foreground"), label);
            Color selection = safeColor(
                UIManager.getColor("TextPane.selectionBackground"),
                safeColor(UIManager.getColor("TextArea.selectionBackground"), new Color(206, 226, 255))
            );
            Color accent = safeColor(UIManager.getColor("Component.accentColor"), buttonBackground);
            Color accentForeground = contrastFor(accent);
            return new ReaderPalette(
                shell,
                shell,
                label,
                meta,
                subtle,
                label,
                selection,
                accent,
                accentForeground,
                buttonBackground,
                buttonForeground
            );
        }

        private static ReaderPalette paper() {
            return new ReaderPalette(
                new Color(240, 237, 229),
                new Color(248, 243, 232),
                new Color(54, 43, 31),
                new Color(104, 90, 73),
                new Color(137, 123, 105),
                new Color(58, 47, 37),
                new Color(230, 223, 208),
                new Color(142, 102, 56),
                Color.WHITE,
                new Color(236, 229, 216),
                new Color(91, 72, 51)
            );
        }

        private static ReaderPalette manualDark() {
            return new ReaderPalette(
                new Color(25, 28, 34),
                new Color(34, 37, 44),
                new Color(235, 239, 244),
                new Color(170, 177, 188),
                new Color(127, 135, 149),
                new Color(227, 219, 205),
                new Color(68, 80, 101),
                new Color(80, 122, 236),
                Color.WHITE,
                new Color(54, 58, 68),
                new Color(218, 224, 231)
            );
        }

        private static ReaderPalette forest() {
            return new ReaderPalette(
                new Color(229, 236, 227),
                new Color(241, 247, 238),
                new Color(46, 63, 43),
                new Color(86, 104, 81),
                new Color(118, 132, 113),
                new Color(42, 58, 39),
                new Color(215, 229, 213),
                new Color(77, 129, 88),
                Color.WHITE,
                new Color(223, 233, 221),
                new Color(55, 78, 51)
            );
        }

        private static Color safeColor(Color color, Color fallback) {
            return color == null ? fallback : color;
        }

        private static Color blend(Color source, Color target, float ratio) {
            int red = Math.round(source.getRed() * ratio + target.getRed() * (1 - ratio));
            int green = Math.round(source.getGreen() * ratio + target.getGreen() * (1 - ratio));
            int blue = Math.round(source.getBlue() * ratio + target.getBlue() * (1 - ratio));
            return new Color(red, green, blue);
        }

        private static Color contrastFor(Color color) {
            double luminance = color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114;
            return luminance >= 150 ? Color.BLACK : Color.WHITE;
        }
    }

    private static final class WrappingFlowLayout extends FlowLayout {
        private WrappingFlowLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= getHgap() + 1;
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth <= 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + getHgap() * 2;
                int maxWidth = targetWidth - horizontalInsetsAndGap;
                if (maxWidth <= 0) {
                    maxWidth = Integer.MAX_VALUE;
                }

                Dimension dimension = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                for (int index = 0; index < target.getComponentCount(); index++) {
                    Component component = target.getComponent(index);
                    if (!component.isVisible()) {
                        continue;
                    }

                    Dimension componentSize = preferred ? component.getPreferredSize() : component.getMinimumSize();
                    if (rowWidth > 0 && rowWidth + getHgap() + componentSize.width > maxWidth) {
                        addRow(dimension, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth > 0) {
                        rowWidth += getHgap();
                    }
                    rowWidth += componentSize.width;
                    rowHeight = Math.max(rowHeight, componentSize.height);
                }

                addRow(dimension, rowWidth, rowHeight);
                dimension.width += horizontalInsetsAndGap;
                dimension.height += insets.top + insets.bottom + getVgap() * 2;
                return dimension;
            }
        }

        private void addRow(Dimension dimension, int rowWidth, int rowHeight) {
            dimension.width = Math.max(dimension.width, rowWidth);
            if (dimension.height > 0) {
                dimension.height += getVgap();
            }
            dimension.height += rowHeight;
        }
    }
}
