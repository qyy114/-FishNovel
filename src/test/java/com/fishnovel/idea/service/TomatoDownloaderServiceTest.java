package com.fishnovel.idea.service;

import com.fishnovel.idea.source.TomatoDownloadResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class TomatoDownloaderServiceTest {
    @Test
    public void shouldCreateJobSubmitChoicesAndLocateDownloadedTxt() throws Exception {
        Path dataDir = Files.createTempDirectory("fishnovel-tomato-service");
        Path libraryDir = dataDir.resolve("library");
        List<String> receivedPaths = new ArrayList<>();
        List<String> receivedBodies = new ArrayList<>();
        AtomicInteger pollCount = new AtomicInteger();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            receivedPaths.add(method + " " + path);

            if ("GET".equals(method) && "/api/status".equals(path)) {
                writeJson(exchange, "{}");
                return;
            }
            if ("POST".equals(method) && "/api/jobs".equals(path)) {
                readBody(exchange);
                Files.createDirectories(libraryDir);
                Files.writeString(
                    libraryDir.resolve("\u6d4b\u8bd5\u756a\u8304.txt"),
                    "\u7b2c\u4e00\u7ae0 \u5f00\u59cb\ncontent",
                    StandardCharsets.UTF_8
                );
                writeJson(exchange, "{\"id\":7,\"book_id\":\"7423591956359416856\"}");
                return;
            }
            if ("GET".equals(method) && "/api/jobs".equals(path)) {
                int currentPoll = pollCount.incrementAndGet();
                if (currentPoll == 1) {
                    writeJson(exchange, "{\"items\":[{\"state\":\"running\",\"format_options\":[\"txt\"],\"book_name_options\":[]}]}");
                    return;
                }
                if (currentPoll == 2) {
                    writeJson(exchange, "{\"items\":[{\"state\":\"running\",\"format_options\":[],\"book_name_options\":[\"A\",\"B\"]}]}");
                    return;
                }
                writeJson(exchange, "{\"items\":[{\"state\":\"done\",\"title\":\"\u6d4b\u8bd5\u756a\u8304\",\"message\":null,\"format_options\":[],\"book_name_options\":[]}]}");
                return;
            }
            if ("POST".equals(method) && "/api/jobs/7/format".equals(path)) {
                receivedBodies.add(readBody(exchange));
                writeJson(exchange, "{}");
                return;
            }
            if ("POST".equals(method) && "/api/jobs/7/book_name".equals(path)) {
                receivedBodies.add(readBody(exchange));
                writeJson(exchange, "{}");
                return;
            }
            writeJson(exchange, 404, "{\"error\":\"not found\"}");
        });
        server.start();

        try {
            URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            TomatoDownloaderService service = new TomatoDownloaderService(
                null,
                dataDir,
                baseUri,
                HttpClient.newHttpClient(),
                (executable, ignoredDataDir, port, logFile) -> {
                    throw new AssertionError("External process should not be launched for fake server tests.");
                },
                1L,
                100L,
                2_000L
            );

            TomatoDownloadResult result = service.download("https://fanqienovel.com/page/7423591956359416856");

            Assert.assertEquals("7423591956359416856", result.getBookId());
            Assert.assertEquals("\u6d4b\u8bd5\u756a\u8304", result.getTitle());
            Assert.assertEquals(libraryDir.resolve("\u6d4b\u8bd5\u756a\u8304.txt").toAbsolutePath().normalize(), result.getTxtPath());
            Assert.assertTrue(receivedPaths.contains("POST /api/jobs/7/format"));
            Assert.assertTrue(receivedPaths.contains("POST /api/jobs/7/book_name"));
            Assert.assertTrue(receivedBodies.toString(), receivedBodies.stream().anyMatch(body -> body.contains("\"txt\"")));
            Assert.assertTrue(receivedBodies.toString(), receivedBodies.stream().anyMatch(body -> body.contains("\"value\":null")));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void shouldStopManagedSidecarAfterDownloadCompletes() throws Exception {
        Path dataDir = Files.createTempDirectory("fishnovel-tomato-managed");
        Path libraryDir = dataDir.resolve("library");
        Path executable = Files.createTempFile("TomatoNovelDownloader-Win64-test", ".exe");
        AtomicReference<FakeProcess> processRef = new AtomicReference<>();
        AtomicReference<HttpServer> serverRef = new AtomicReference<>();

        TomatoDownloaderService service = new TomatoDownloaderService(
            null,
            dataDir,
            null,
            HttpClient.newHttpClient(),
            (ignoredExecutable, ignoredDataDir, port, logFile) -> {
                Assert.assertEquals(executable.toAbsolutePath().normalize(), ignoredExecutable.toAbsolutePath().normalize());
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
                server.createContext("/", exchange -> {
                    String method = exchange.getRequestMethod();
                    String path = exchange.getRequestURI().getPath();
                    if ("GET".equals(method) && "/api/status".equals(path)) {
                        writeJson(exchange, "{}");
                        return;
                    }
                    if ("POST".equals(method) && "/api/jobs".equals(path)) {
                        readBody(exchange);
                        Files.createDirectories(libraryDir);
                        Files.writeString(
                            libraryDir.resolve("\u6253\u5f00\u5373\u5173\u95ed.txt"),
                            "\u7b2c\u4e00\u7ae0 \u5f00\u59cb\ncontent",
                            StandardCharsets.UTF_8
                        );
                        writeJson(exchange, "{\"id\":8,\"book_id\":\"7423591956359416856\"}");
                        return;
                    }
                    if ("GET".equals(method) && "/api/jobs".equals(path)) {
                        writeJson(exchange, "{\"items\":[{\"state\":\"done\",\"title\":\"\u6253\u5f00\u5373\u5173\u95ed\",\"message\":null,\"format_options\":[],\"book_name_options\":[]}]}");
                        return;
                    }
                    writeJson(exchange, 404, "{\"error\":\"not found\"}");
                });
                server.start();
                serverRef.set(server);
                FakeProcess process = new FakeProcess(() -> {
                    HttpServer runningServer = serverRef.get();
                    if (runningServer != null) {
                        runningServer.stop(0);
                    }
                });
                processRef.set(process);
                return process;
            },
            1L,
            500L,
            2_000L,
            executable
        );

        TomatoDownloadResult result = service.download("7423591956359416856");

        Assert.assertEquals("7423591956359416856", result.getBookId());
        Assert.assertNotNull(processRef.get());
        Assert.assertFalse(processRef.get().isAlive());
    }

    @Test
    public void shouldOnlyCleanFilesInsideTomatoDataDirectory() throws Exception {
        Path dataDir = Files.createTempDirectory("fishnovel-tomato-clean");
        Path libraryDir = dataDir.resolve("library");
        Path targetDir = libraryDir.resolve("7423591956359416856_cached");
        Path targetTxt = targetDir.resolve("book.txt");
        Path outsideTxt = Files.createTempFile("fishnovel-tomato-outside", ".txt");
        Files.createDirectories(targetDir);
        Files.writeString(targetTxt, "inside", StandardCharsets.UTF_8);
        Files.writeString(outsideTxt, "outside", StandardCharsets.UTF_8);
        Files.writeString(
            dataDir.resolve("fishnovel-tomato-books.json"),
            "{\"books\":{\"7423591956359416856\":{\"bookId\":\"7423591956359416856\",\"title\":\"T\",\"txtPath\":\""
                + outsideTxt.toAbsolutePath().normalize().toString().replace("\\", "\\\\")
                + "\",\"updatedAt\":1}}}",
            StandardCharsets.UTF_8
        );

        TomatoDownloaderService service = new TomatoDownloaderService(
            null,
            dataDir,
            URI.create("http://127.0.0.1:1"),
            HttpClient.newHttpClient(),
            (executable, ignoredDataDir, port, logFile) -> {
                throw new AssertionError("External process should not be launched for cleanup tests.");
            },
            1L,
            100L,
            100L
        );

        service.cleanupBookCache("7423591956359416856");

        Assert.assertFalse(Files.exists(targetDir));
        Assert.assertTrue(Files.exists(outsideTxt));
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void writeJson(HttpExchange exchange, String json) throws IOException {
        writeJson(exchange, 200, json);
    }

    private static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static final class FakeProcess extends Process {
        private final Runnable onDestroy;
        private volatile boolean alive = true;

        private FakeProcess(Runnable onDestroy) {
            this.onDestroy = onDestroy;
        }

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            alive = false;
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return !alive;
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("Process is still running.");
            }
            return 0;
        }

        @Override
        public void destroy() {
            if (!alive) {
                return;
            }
            alive = false;
            onDestroy.run();
        }

        @Override
        public Process destroyForcibly() {
            destroy();
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }
    }
}
