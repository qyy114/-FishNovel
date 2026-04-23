package com.fishnovel.idea.service;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

final class LocalBookPathResolver {
    private LocalBookPathResolver() {
    }

    static Path resolve(String sourceLocation) throws IOException {
        if (sourceLocation == null || sourceLocation.isBlank()) {
            throw new IOException("这本小说没有可用的本地文件路径，请重新导入。");
        }
        try {
            return Path.of(sourceLocation);
        } catch (InvalidPathException ex) {
            throw new IOException(
                "这本小说保存的本地路径已经损坏，通常是旧版本乱码记录造成的。\n请重新导入原文件后再阅读。\n当前记录路径：" + sourceLocation,
                ex
            );
        }
    }
}
