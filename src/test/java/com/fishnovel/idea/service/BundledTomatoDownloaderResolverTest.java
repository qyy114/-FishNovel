package com.fishnovel.idea.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class BundledTomatoDownloaderResolverTest {
    @Test
    public void shouldExtractBundledDownloaderWhenExternalPathIsMissing() throws Exception {
        Path runtimeDir = Files.createTempDirectory("fishnovel-bundled-tomato");
        BundledTomatoDownloaderResolver resolver = new BundledTomatoDownloaderResolver(
            (BundledTomatoDownloaderResolver.ExternalPathStore) null,
            runtimeDir,
            new ResourceClassLoader()
        );

        Path resolved = resolver.resolve().orElseThrow();

        Assert.assertEquals(
            runtimeDir.resolve(BundledTomatoDownloaderResolver.BUNDLED_EXE_NAME).toAbsolutePath().normalize(),
            resolved
        );
        Assert.assertEquals("fake-exe", Files.readString(resolved, StandardCharsets.UTF_8));
        Assert.assertTrue(Files.isRegularFile(runtimeDir.resolve(BundledTomatoDownloaderResolver.LICENSE_RESOURCE_NAME)));
    }

    @Test
    public void shouldPreferConfiguredExternalDownloaderPath() throws Exception {
        Path runtimeDir = Files.createTempDirectory("fishnovel-external-tomato");
        Path externalExe = Files.createTempFile("TomatoNovelDownloader-Win64-external", ".exe");

        BundledTomatoDownloaderResolver resolver = new BundledTomatoDownloaderResolver(
            new MemoryExternalPathStore(externalExe.toString()),
            runtimeDir,
            new MissingResourceClassLoader()
        );

        Assert.assertEquals(externalExe.toAbsolutePath().normalize(), resolver.resolve().orElseThrow());
    }

    @Test
    public void shouldReportMissingBundledDownloaderResource() throws Exception {
        Path runtimeDir = Files.createTempDirectory("fishnovel-missing-tomato");
        BundledTomatoDownloaderResolver resolver = new BundledTomatoDownloaderResolver(
            (BundledTomatoDownloaderResolver.ExternalPathStore) null,
            runtimeDir,
            new MissingResourceClassLoader()
        );

        IOException error = Assert.assertThrows(IOException.class, resolver::resolve);

        Assert.assertTrue(error.getMessage().contains("resource is missing"));
        Assert.assertTrue(error.getMessage().contains(BundledTomatoDownloaderResolver.BUNDLED_EXE_NAME));
    }

    private static final class ResourceClassLoader extends ClassLoader {
        @Override
        public InputStream getResourceAsStream(String name) {
            if ((BundledTomatoDownloaderResolver.RESOURCE_BASE + "/" + BundledTomatoDownloaderResolver.BUNDLED_EXE_NAME).equals(name)) {
                return new ByteArrayInputStream("fake-exe".getBytes(StandardCharsets.UTF_8));
            }
            if ((BundledTomatoDownloaderResolver.RESOURCE_BASE + "/" + BundledTomatoDownloaderResolver.LICENSE_RESOURCE_NAME).equals(name)) {
                return new ByteArrayInputStream("license".getBytes(StandardCharsets.UTF_8));
            }
            return null;
        }
    }

    private static final class MissingResourceClassLoader extends ClassLoader {
        @Override
        public InputStream getResourceAsStream(String name) {
            return null;
        }
    }

    private static final class MemoryExternalPathStore implements BundledTomatoDownloaderResolver.ExternalPathStore {
        private String path;

        private MemoryExternalPathStore(String path) {
            this.path = path;
        }

        @Override
        public Optional<String> get() {
            return Optional.ofNullable(path);
        }

        @Override
        public void set(String path) {
            this.path = path;
        }
    }
}
