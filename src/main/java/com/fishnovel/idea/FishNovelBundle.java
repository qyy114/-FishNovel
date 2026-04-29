package com.fishnovel.idea;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class FishNovelBundle extends DynamicBundle {
    @NonNls
    public static final String BUNDLE_NAME = "messages.FishNovelBundle";

    private static final FishNovelBundle INSTANCE = new FishNovelBundle();

    private FishNovelBundle() {
        super(BUNDLE_NAME);
    }

    public static @NotNull String message(
        @NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key,
        Object @NotNull ... params
    ) {
        return INSTANCE.getMessage(key, params);
    }
}
