package com.fishnovel.idea.ui;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.Bookmark;
import com.fishnovel.idea.model.Chapter;
import com.fishnovel.idea.model.ReaderTheme;
import com.fishnovel.idea.model.ReadingPreferences;
import com.fishnovel.idea.model.ReadingProgress;
import com.fishnovel.idea.service.ReadingProgressResolver;
import com.fishnovel.idea.service.ReadingStateService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
    private final ReadingStateService stateService;
    private final Runnable onStateChanged;

    private final JBLabel titleLabel = new JBLabel("FishNovel");
    private final JBLabel chapterMetaLabel = new JBLabel("导入一本小说开始阅读");
    private final JBLabel sourceLabel = new JBLabel("支持 TXT / EPUB / Markdown / HTML / 网页章节");
    private final JComboBox<Chapter> chapterSelector = new JComboBox<>();
    private final JComboBox<ReaderTheme> themeSelector = new JComboBox<>(ReaderTheme.values());
    private final JTextPane textPane = new JTextPane();
    private final JScrollPane scrollPane = new JScrollPane(textPane);
    private final JPanel readerShell = new JPanel(new GridBagLayout());
    private final JPanel readerCard = new JPanel(new BorderLayout());
    private final JButton bookmarkButton = new JButton("添加书签");
    private final JButton fontMinusButton = new JButton("A-");
    private final JButton fontPlusButton = new JButton("A+");
    private final JButton spacingMinusButton = new JButton("紧凑");
    private final JButton spacingPlusButton = new JButton("舒展");

    private BookDocument currentDocument;
    private boolean adjusting;

    public BookReaderPanel(Project project, Runnable onStateChanged) {
        super(new BorderLayout());
        this.project = project;
        this.stateService = ReadingStateService.getInstance();
        this.onStateChanged = onStateChanged;

        buildUi();
        registerListeners();
        clearBook();
    }

    private void buildUi() {
        setBorder(JBUI.Borders.empty());

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(JBUI.Borders.emptyBottom(10));
        header.setOpaque(false);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        chapterMetaLabel.setFont(chapterMetaLabel.getFont().deriveFont(Font.PLAIN, 12f));
        sourceLabel.setFont(sourceLabel.getFont().deriveFont(Font.PLAIN, 11f));

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(chapterMetaLabel);
        header.add(Box.createVerticalStrut(2));
        header.add(sourceLabel);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(new JBLabel("章节"));
        chapterSelector.setPreferredSize(new Dimension(220, 30));
        controls.add(chapterSelector);
        controls.add(fontMinusButton);
        controls.add(fontPlusButton);
        controls.add(spacingMinusButton);
        controls.add(spacingPlusButton);
        controls.add(new JBLabel("主题"));
        controls.add(themeSelector);
        controls.add(bookmarkButton);

        JPanel topPanel = new JPanel(new BorderLayout(0, 8));
        topPanel.setOpaque(false);
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(controls, BorderLayout.SOUTH);

        textPane.setEditable(false);
        textPane.setBorder(new EmptyBorder(28, 34, 32, 34));
        textPane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        textPane.setOpaque(true);

        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(28);
        scrollPane.setOpaque(true);

        readerCard.add(scrollPane, BorderLayout.CENTER);
        readerCard.setPreferredSize(new Dimension(780, 0));
        readerCard.setBorder(BorderFactory.createEmptyBorder());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        readerShell.add(readerCard, constraints);
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
        sourceLabel.setText(formatSourceLine(document));

        chapterSelector.removeAllItems();
        for (Chapter chapter : document.getChapters()) {
            chapterSelector.addItem(chapter);
        }

        ReadingPreferences preferences = stateService.getPreferences(document.getBookId());
        themeSelector.setSelectedItem(preferences.getTheme());
        applyPreferences(preferences);

        ReadingProgress progress = overrideProgress == null ? stateService.getProgress(document.getBookId()) : overrideProgress;
        int chapterIndex = ReadingProgressResolver.resolveChapterIndex(document, progress);
        chapterSelector.setSelectedIndex(chapterIndex);
        adjusting = false;
        renderChapter(chapterIndex, progress.getContentOffset());
    }

    public void clearBook() {
        currentDocument = null;
        adjusting = true;
        titleLabel.setText("FishNovel");
        chapterMetaLabel.setText("导入一本小说开始阅读");
        sourceLabel.setText("支持 TXT / EPUB / Markdown / HTML / 网页章节");
        chapterSelector.removeAllItems();
        textPane.setText("");
        applyPreferences(ReadingPreferences.defaults());
        adjusting = false;
    }

    public boolean isShowingBook(String bookId) {
        return currentDocument != null && currentDocument.getBookId().equals(bookId);
    }

    private String formatSourceLine(BookDocument document) {
        String source = document.getSourceLocation();
        if (source == null || source.isBlank()) {
            return "来源：FishNovel";
        }
        return "来源：" + source;
    }

    private void renderChapter(int chapterIndex, int contentOffset) {
        if (currentDocument == null) {
            return;
        }

        Chapter chapter = currentDocument.getChapter(chapterIndex);
        adjusting = true;
        chapterMetaLabel.setText("第 " + (chapterIndex + 1) + " / " + currentDocument.getChapters().size() + " 章 · " + chapter.getTitle());
        StyledDocument styledDocument = textPane.getStyledDocument();
        try {
            styledDocument.remove(0, styledDocument.getLength());
            styledDocument.insertString(0, chapter.getContent(), null);
            applyParagraphStyle(getCurrentPreferences());
        } catch (BadLocationException ex) {
            Messages.showErrorDialog(project, "渲染章节失败：\n" + ex.getMessage(), "FishNovel");
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
        Messages.showInfoMessage(project, "已添加书签：\n" + bookmark.getChapterTitle(), "FishNovel");
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

        titleLabel.setForeground(palette.titleColor);
        chapterMetaLabel.setForeground(palette.metaColor);
        sourceLabel.setForeground(palette.subtleMetaColor);

        scrollPane.setBackground(palette.cardBackground);
        scrollPane.getViewport().setBackground(palette.cardBackground);
        textPane.setBackground(palette.cardBackground);
        textPane.setForeground(palette.textColor);
        textPane.setCaretColor(palette.textColor);
        textPane.setSelectionColor(palette.selectionColor);
        textPane.setSelectedTextColor(palette.textColor);

        styleButton(bookmarkButton, palette, true);
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
        StyleConstants.setFirstLineIndent(attributes, fontSize);
        document.setParagraphAttributes(0, document.getLength(), attributes, false);
    }

    private void styleButton(JButton button, ReaderPalette palette, boolean primary) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        Font systemFont = resolveSystemFont();
        button.setFont(systemFont.deriveFont(Font.PLAIN, systemFont.getSize2D()));
        button.setBorder(JBUI.Borders.empty(7, 12));
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
}
