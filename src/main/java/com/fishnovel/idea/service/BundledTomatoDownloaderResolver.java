package com.fishnovel.idea.service;

import com.intellij.openapi.application.PathManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;

final class BundledTomatoDownloaderResolver implements TomatoDownloaderResolver {
    static final String BUNDLED_TOMATO_VERSION = "v2.4.9";
    static final String BUNDLED_EXE_NAME = "TomatoNovelDownloader-Win64-v2.4.9.exe";
    static final String RESOURCE_BASE = "tomato/windows-x64";
    static final String LICENSE_RESOURCE_NAME = "Tomato-Novel-Downloader-LICENSE.txt";

    private final ExternalPathStore externalPathStore;
    private final Path runtimeDir;
    private final ClassLoader classLoader;

    BundledTomatoDownloaderResolver(ReadingStateService stateService) {
        this(
            stateService,
            Path.of(PathManager.getSystemPath(), "FishNovel", "tomato", "runtime", BUNDLED_TOMATO_VERSION),
            BundledTomatoDownloaderResolver.class.getClassLoader()
        );
    }

    BundledTomatoDownloaderResolver(ReadingStateService stateService, Path runtimeDir, ClassLoader classLoader) {
        this(new ReadingStateExternalPathStore(stateService), runtimeDir, classLoader);
    }

    BundledTomatoDownloaderResolver(ExternalPathStore externalPathStore, Path runtimeDir, ClassLoader classLoader) {
        this.externalPathStore = externalPathStore == null ? new EmptyExternalPathStore() : externalPathStore;
        this.runtimeDir = runtimeDir.toAbsolutePath().normalize();
        this.classLoader = classLoader;
    }

    @Override
    public Optional<Path> resolve() throws IOException {
        Optional<Path> externalPath = getExternalPath();
        if (externalPath.filter(Files::isRegularFile).isPresent()) {
            return externalPath;
        }
        if (!isWindowsX64()) {
            return Optional.empty();
        }
        return Optional.of(extractBundledDownloader());
    }

    @Override
    public void setExternalPath(Path path) {
        externalPathStore.set(path == null ? null : path.toAbsolutePath().normalize().toString());
    }

    Optional<Path> getExternalPath() {
        return externalPathStore.get()
            .map(Path::of)
            .map(path -> path.toAbsolutePath().normalize());
    }

    private Path extractBundledDownloader() throws IOException {
        Files.createDirectories(runtimeDir);
        Path executable = runtimeDir.resolve(BUNDLED_EXE_NAME).toAbsolutePath().normalize();
        copyResourceIfNeeded(resourcePath(BUNDLED_EXE_NAME), executable);
        copyResourceIfNeeded(resourcePath(LICENSE_RESOURCE_NAME), runtimeDir.resolve(LICENSE_RESOURCE_NAME));
        if (!Files.isRegularFile(executable) || Files.size(executable) <= 0L) {
            throw new IOException("Bundled Tomato-Novel-Downloader is missing or empty: " + executable);
        }
        return executable;
    }

    private void copyResourceIfNeeded(String resourcePath, Path target) throws IOException {
        if (Files.isRegularFile(target) && Files.size(target) > 0L) {
            return;
        }
        try (InputStream input = classLoader.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("Bundled Tomato-Novel-Downloader resource is missing: " + resourcePath);
            }
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String resourcePath(String fileName) {
        return RESOURCE_BASE + "/" + fileName;
    }

    private static boolean isWindowsX64() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean windows = osName.contains("win");
        boolean x64 = osArch.equals("amd64") || osArch.equals("x86_64");
        return windows && x64;
    }

    interface ExternalPathStore {
        Optional<String> get();

        void set(String path);
    }

    private static final class ReadingStateExternalPathStore implements ExternalPathStore {
        private final ReadingStateService stateService;

        private ReadingStateExternalPathStore(ReadingStateService stateService) {
            this.stateService = stateService;
        }

        @Override
        public Optional<String> get() {
            if (stateService == null) {
                return Optional.empty();
            }
            return stateService.getTomatoDownloaderPath();
        }

        @Override
        public void set(String path) {
            if (stateService == null) {
                throw new IllegalStateException("Tomato downloader path storage is not available.");
            }
            stateService.setTomatoDownloaderPath(path);
        }
    }

    private static final class EmptyExternalPathStore implements ExternalPathStore {
        @Override
        public Optional<String> get() {
            return Optional.empty();
        }

        @Override
        public void set(String path) {
            throw new IllegalStateException("Tomato downloader path storage is not available.");
        }
    }
}
