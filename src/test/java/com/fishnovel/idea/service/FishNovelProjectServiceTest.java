package com.fishnovel.idea.service;

import java.io.IOException;
import java.net.URI;
import org.junit.Assert;
import org.junit.Test;

public class FishNovelProjectServiceTest {
    @Test
    public void shouldShowFriendlyMessageForInvalidStoredWindowsPath() {
        String sourceLocation = "D:\\360瀹夊叏娴忚鍣ㄤ笅杞絓\\涓夊浗婕斾箟_绗竴鍗穃\\绗?绔燶\\20绔?.txt";
        IOException error = Assert.assertThrows(IOException.class, () -> LocalBookPathResolver.resolve(sourceLocation));

        Assert.assertTrue(error.getMessage().contains("本地路径已经损坏"));
        Assert.assertTrue(error.getMessage().contains("请重新导入原文件后再阅读"));
    }

    @Test
    public void shouldRejectUnexpectedRedirectToAnotherHost() {
        Assert.assertTrue(
            RemoteRedirectPolicy.isUnexpectedRedirect(
                URI.create("https://www.sudugu.org/5/20.html"),
                URI.create("https://www.google.com.hk/")
            )
        );
    }

    @Test
    public void shouldAllowRedirectWithinSameHostFamily() {
        Assert.assertFalse(
            RemoteRedirectPolicy.isUnexpectedRedirect(
                URI.create("https://www.sudugu.org/5/20.html"),
                URI.create("https://m.sudugu.org/5/20.html")
            )
        );
    }
}
