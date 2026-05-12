package com.fishnovel.idea.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

interface TomatoDownloaderResolver {
    Optional<Path> resolve() throws IOException;

    void setExternalPath(Path path);
}
