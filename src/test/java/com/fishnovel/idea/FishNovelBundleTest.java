package com.fishnovel.idea;

import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.Assert;
import org.junit.Test;

public class FishNovelBundleTest {
    private static final String BUNDLE_NAME = "messages.FishNovelBundle";
    private static final ResourceBundle.Control NO_FALLBACK =
        ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    @Test
    public void shouldExposeBundleNameForPluginHelper() {
        Assert.assertEquals(BUNDLE_NAME, FishNovelBundle.BUNDLE_NAME);
    }

    @Test
    public void shouldLoadDefaultAndChineseBundlesWithSameKeys() {
        ResourceBundle defaultBundle = bundle(Locale.ROOT);
        ResourceBundle chineseBundle = bundle(Locale.SIMPLIFIED_CHINESE);

        Assert.assertEquals(defaultBundle.keySet(), chineseBundle.keySet());
    }

    @Test
    public void shouldExposeRepresentativeDefaultMessages() {
        ResourceBundle bundle = bundle(Locale.ROOT);

        Assert.assertEquals("Open FishNovel", bundle.getString("action.FishNovel.OpenToolWindowAction.text"));
        Assert.assertEquals("Import Novel", bundle.getString("toolbar.importBook"));
        Assert.assertEquals("Follow System", bundle.getString("reader.theme.auto"));
        Assert.assertEquals("Chapter {0} / {1} - {2}", bundle.getString("reader.chapterMeta"));
        Assert.assertEquals("{0} bookmarks", bundle.getString("sidebar.bookmarkCount"));
    }

    @Test
    public void shouldExposeRepresentativeChineseMessages() {
        ResourceBundle bundle = bundle(Locale.SIMPLIFIED_CHINESE);

        Assert.assertEquals("打开 FishNovel", bundle.getString("action.FishNovel.OpenToolWindowAction.text"));
        Assert.assertEquals("导入小说", bundle.getString("toolbar.importBook"));
        Assert.assertEquals("跟随系统", bundle.getString("reader.theme.auto"));
        Assert.assertEquals("第 {0} / {1} 章 · {2}", bundle.getString("reader.chapterMeta"));
        Assert.assertEquals("{0} 个书签", bundle.getString("sidebar.bookmarkCount"));
    }

    private ResourceBundle bundle(Locale locale) {
        return ResourceBundle.getBundle(BUNDLE_NAME, locale, NO_FALLBACK);
    }
}
