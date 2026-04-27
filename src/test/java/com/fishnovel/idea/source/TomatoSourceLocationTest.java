package com.fishnovel.idea.source;

import org.junit.Assert;
import org.junit.Test;

public class TomatoSourceLocationTest {
    @Test
    public void shouldBuildAndParseTomatoLocation() {
        String location = TomatoSourceLocation.toLocation("7423591956359416856");

        Assert.assertEquals("tomato://book/7423591956359416856", location);
        Assert.assertEquals(
            "7423591956359416856",
            TomatoSourceLocation.parseBookId(location).orElseThrow()
        );
    }

    @Test
    public void shouldRejectNonTomatoOrNonNumericLocations() {
        Assert.assertTrue(TomatoSourceLocation.parseBookId("file:///tmp/book.txt").isEmpty());
        Assert.assertTrue(TomatoSourceLocation.parseBookId("tomato://book/abc").isEmpty());
        Assert.assertTrue(TomatoSourceLocation.normalizeBookId("https://example.com/book/742").isEmpty());
    }
}
