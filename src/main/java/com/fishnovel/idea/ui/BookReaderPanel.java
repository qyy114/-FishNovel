package com.fishnovel.idea.ui;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Bookmark;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.ReaderTheme;
import com.fishnovel.idea.model.ReadingPreferences;
import com.fishnovel.idea.model.ReadingProgress;
import com.fishnovel.idea.service.FishNovelProjectService;
import com.fishnovel.idea.service.ReadingStateService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public final class BookReaderPanel extends JPanel {
    private final Project project;
    private final ReadingStateService stateService;
    private final FishNovelProjectService projectService;
    private final Runnable onStateChanged;
    private final JLabel titleLabel = new JLabel("FishNovel");
    private final JLabel sourceLabel = new JLabel("导入一本小说开始阅读");
    private final JComboBox<Chapter> chapterSelector = new JComboBox<>();
    private final JComboBox<ReaderTheme> themeSelector = new JComboBox<>(ReaderTheme.values());
    private final JTextPane textPane = new JTextPane();
    private final JScrollPane scrollPane = new JScrollPane(textPane);
    private final JButton bookmarkButton = new JButton("加书签");
    private final JButton openInEditorButton = new JButton("在编辑器打开");
    private final JButton fontMinusButton = new JButton("A-");
    private final JButton fontPlusButton = new JButton("A+");
    private final JButton spacingMinusButton = new JButton("行距-");
    private final JButton spacingPlusButton = new JButton("行距+");
    private BookDocument currentDocument;
    private boolean adjusting;

    public BookReaderPanel(Project project, Runnable onStateChanged) {
        super(new BorderLayout(0, 12));
        this.project = project;
        this.stateService = ReadingStateService.getInstance();
        this.projectService = FishNovelProjectService.getInstance(project);
        this.onStateChanged = onStateChanged;

        buildUi();
        registerListeners();
    }

    private void buildUi() {
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        sourceLabel.setFont(sourceLabel.getFont().deriveFont(12f));
        header.add(titleLabel);
        header.add(sourceLabel);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.add(new JLabel("章节"));
        controls.add(chapterSelector);
        controls.add(fontMinusButton);
        controls.add(fontPlusButton);
        controls.add(spacingMinusButton);
        controls.add(spacingPlusButton);
        controls.add(themeSelector);
        controls.add(bookmarkButton);
        controls.add(openInEditorButton);

        JPanel topPanel = new JPanel(new BorderLayout(0, 8));
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(controls, BorderLayout.SOUTH);

        textPane.setEditable(false);
        textPane.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void registerListeners() {
        chapterSelector.addActionListener(event -> {
            if (!adjusting && currentDocument != null && chapterSelector.getSelectedIndex() >= 0) {
                renderChapter(chapterSelector.getSelectedIndex(), 0);
            }
        });

        themeSelector.addActionListener(event -> {
            if (!adjusting && currentDocument != null) {
                updatePreferences(getCurrentPreferences().getFontSize(), getCurrentPreferences().getLineSpacing(), (ReaderTheme) themeSelector.getSelectedItem());
            }
        });

        fontMinusButton.addActionListener(event -> shiftFontSize(-1));
        fontPlusButton.addActionListener(event -> shiftFontSize(1));
        spacingMinusButton.addActionListener(event -> shiftLineSpacing(-0.05f));
        spacingPlusButton.addActionListener(event -> shiftLineSpacing(0.05f));

        bookmarkButton.addActionListener(event -> addBookmark());
        openInEditorButton.addActionListener(event -> {
            if (currentDocument != null) {
                projectService.openInEditor(currentDocument, currentProgressSnapshot());
            }
        });

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
        currentDocument = document;
        adjusting = true;
        stateService.registerBook(document);

        titleLabel.setText(document.getTitle());
        sourceLabel.setText(document.getSourceLocation());

        chapterSelector.removeAllItems();
        for (Chapter chapter : document.getChapters()) {
            chapterSelector.addItem(chapter);
        }

        ReadingPreferences preferences = stateService.getPreferences(document.getBookId());
        themeSelector.setSelectedItem(preferences.getTheme());
        applyPreferences(preferences);

        ReadingProgress progress = overrideProgress == null ? stateService.getProgress(document.getBookId()) : overrideProgress;
        int chapterIndex = Math.max(0, Math.min(progress.getChapterIndex(), document.getChapters().size() - 1));
        chapterSelector.setSelectedIndex(chapterIndex);
        adjusting = false;
        renderChapter(chapterIndex, progress.getContentOffset());
    }

    private void renderChapter(int chapterIndex, int contentOffset) {
        if (currentDocument == null) {
            return;
        }

        Chapter chapter = currentDocument.getChapter(chapterIndex);
        adjusting = true;
        StyledDocument styledDocument = textPane.getStyledDocument();
        try {
            styledDocument.remove(0, styledDocument.getLength());
            styledDocument.insertString(0, chapter.getContent(), null);
            applyParagraphStyle(getCurrentPreferences());
        } catch (BadLocationException e) {
            Messages.showErrorDialog(project, "渲染章节失败：\n" + e.getMessage(), "FishNovel");
        }

        restoreOffset(contentOffset);
        adjusting = false;
        persistProgress(chapterIndex, contentOffset);
    }

    private void restoreOffset(int contentOffset) {
        SwingUtilities.invokeLater(() -> {
            try {
                int safeOffset = Math.max(0, Math.min(contentOffset, textPane.getDocument().getLength()));
                Rectangle2D rectangle = textPane.modelToView2D(safeOffset);
                JViewport viewport = scrollPane.getViewport();
                if (rectangle != null) {
                    viewport.setViewPosition(new Point(0, Math.max(0, (int) rectangle.getY())));
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
        stateService.updateProgress(currentDocument.getBookId(), new ReadingProgress(
            Math.max(0, chapterIndex),
            Math.max(0, contentOffset),
            System.currentTimeMillis()
        ));
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
        Messages.showInfoMessage(project, "书签已添加：\n" + bookmark.getChapterTitle(), "FishNovel");
    }

    private int currentOffset() {
        Point point = scrollPane.getViewport().getViewPosition();
        return Math.max(0, textPane.viewToModel2D(point));
    }

    private ReadingProgress currentProgressSnapshot() {
        return new ReadingProgress(
            Math.max(0, chapterSelector.getSelectedIndex()),
            currentOffset(),
            System.currentTimeMillis()
        );
    }

    private void shiftFontSize(int delta) {
        ReadingPreferences preferences = getCurrentPreferences();
        updatePreferences(Math.max(12, Math.min(32, preferences.getFontSize() + delta)), preferences.getLineSpacing(), preferences.getTheme());
    }

    private void shiftLineSpacing(float delta) {
        ReadingPreferences preferences = getCurrentPreferences();
        float next = Math.max(0f, Math.min(1f, preferences.getLineSpacing() + delta));
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
        textPane.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, preferences.getFontSize()));
        themeSelector.setSelectedItem(preferences.getTheme());
        Color background;
        Color foreground;
        switch (preferences.getTheme()) {
            case DARK -> {
                background = new Color(30, 36, 48);
                foreground = new Color(235, 224, 200);
            }
            case FOREST -> {
                background = new Color(235, 244, 227);
                foreground = new Color(33, 48, 27);
            }
            case PAPER -> {
                background = new Color(246, 237, 219);
                foreground = new Color(44, 37, 25);
            }
            default -> throw new IllegalStateException("Unexpected theme");
        }
        setBackground(background);
        scrollPane.getViewport().setBackground(background);
        textPane.setBackground(background);
        textPane.setForeground(foreground);
    }

    private void applyParagraphStyle(ReadingPreferences preferences) {
        StyledDocument document = textPane.getStyledDocument();
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributes, "Microsoft YaHei UI");
        StyleConstants.setFontSize(attributes, preferences.getFontSize());
        StyleConstants.setLineSpacing(attributes, preferences.getLineSpacing());
        document.setParagraphAttributes(0, document.getLength(), attributes, false);
    }

    public JComponent getPreferredFocusedComponent() {
        return textPane;
    }

    public void disposePanel() {
    }
}
