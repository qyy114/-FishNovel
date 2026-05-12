package com.fishnovel.idea.service;

import com.fishnovel.idea.source.TomatoDownloadResult;
import com.fishnovel.idea.source.TomatoSourceLocation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.PathManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class TomatoDownloaderService {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(20);
    private static final long DEFAULT_POLL_INTERVAL_MILLIS = 500L;
    private static final long DEFAULT_START_TIMEOUT_MILLIS = 20_000L;
    private static final long DEFAULT_JOB_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Path dataDir;
    private final Path libraryDir;
    private final Path metadataFile;
    private final HttpClient httpClient;
    private final SidecarLauncher sidecarLauncher;
    private final long pollIntervalMillis;
    private final long startTimeoutMillis;
    private final long jobTimeoutMillis;
    private final boolean externalServer;
    private final TomatoDownloaderResolver downloaderResolver;

    private URI serverBaseUri;
    private Process sidecarProcess;

    public TomatoDownloaderService(ReadingStateService stateService) {
        this(
            stateService,
            defaultDataDir(),
            null,
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
            new ProcessSidecarLauncher(),
            DEFAULT_POLL_INTERVAL_MILLIS,
            DEFAULT_START_TIMEOUT_MILLIS,
            DEFAULT_JOB_TIMEOUT_MILLIS,
            new BundledTomatoDownloaderResolver(stateService)
        );
    }

    TomatoDownloaderService(
        ReadingStateService stateService,
        Path dataDir,
        URI serverBaseUri,
        HttpClient httpClient,
        SidecarLauncher sidecarLauncher,
        long pollIntervalMillis,
        long startTimeoutMillis,
        long jobTimeoutMillis
    ) {
        this(
            stateService,
            dataDir,
            serverBaseUri,
            httpClient,
            sidecarLauncher,
            pollIntervalMillis,
            startTimeoutMillis,
            jobTimeoutMillis,
            stateService == null ? new FixedTomatoDownloaderResolver(null) : new BundledTomatoDownloaderResolver(stateService)
        );
    }

    TomatoDownloaderService(
        ReadingStateService stateService,
        Path dataDir,
        URI serverBaseUri,
        HttpClient httpClient,
        SidecarLauncher sidecarLauncher,
        long pollIntervalMillis,
        long startTimeoutMillis,
        long jobTimeoutMillis,
        Path downloaderPathOverride
    ) {
        this(
            stateService,
            dataDir,
            serverBaseUri,
            httpClient,
            sidecarLauncher,
            pollIntervalMillis,
            startTimeoutMillis,
            jobTimeoutMillis,
            new FixedTomatoDownloaderResolver(downloaderPathOverride)
        );
    }

    TomatoDownloaderService(
        ReadingStateService stateService,
        Path dataDir,
        URI serverBaseUri,
        HttpClient httpClient,
        SidecarLauncher sidecarLauncher,
        long pollIntervalMillis,
        long startTimeoutMillis,
        long jobTimeoutMillis,
        TomatoDownloaderResolver downloaderResolver
    ) {
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.libraryDir = this.dataDir.resolve("library").normalize();
        this.metadataFile = this.dataDir.resolve("fishnovel-tomato-books.json").normalize();
        this.serverBaseUri = serverBaseUri;
        this.httpClient = httpClient;
        this.sidecarLauncher = sidecarLauncher;
        this.pollIntervalMillis = pollIntervalMillis;
        this.startTimeoutMillis = startTimeoutMillis;
        this.jobTimeoutMillis = jobTimeoutMillis;
        this.externalServer = serverBaseUri != null;
        this.downloaderResolver = downloaderResolver;
    }

    public boolean hasValidDownloaderPath() {
        try {
            return getDownloaderPath()
                .map(Files::isRegularFile)
                .orElse(false);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public Optional<Path> getDownloaderPath() {
        if (downloaderResolver == null) {
            return Optional.empty();
        }
        try {
            return downloaderResolver.resolve();
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    public void setDownloaderPath(Path path) {
        downloaderResolver.setExternalPath(path);
    }

    public Path getLogFile() {
        return dataDir.resolve("tomato-downloader.log").toAbsolutePath().normalize();
    }

    public synchronized TomatoDownloadResult download(String input) throws IOException {
        try {
            return downloadInternal(input, null);
        } finally {
            stopSidecarProcess();
        }
    }

    public synchronized TomatoDownloadResult refresh(String bookId) throws IOException {
        String normalizedBookId = TomatoSourceLocation.normalizeBookId(bookId)
            .orElseThrow(() -> new IOException("Invalid Tomato book id: " + bookId));
        cleanupBookCache(normalizedBookId);
        try {
            return downloadInternal(normalizedBookId, normalizedBookId);
        } finally {
            stopSidecarProcess();
        }
    }

    public TomatoDownloadResult cached(String bookId) throws IOException {
        String normalizedBookId = TomatoSourceLocation.normalizeBookId(bookId)
            .orElseThrow(() -> new IOException("Invalid Tomato book id: " + bookId));
        StoredTomatoBook record = readCache().books.get(normalizedBookId);
        if (record == null || record.txtPath == null || record.txtPath.isBlank()) {
            throw new IOException("找不到番茄小说缓存，请重新使用“番茄下载”。");
        }
        Path txtPath = Path.of(record.txtPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(txtPath)) {
            throw new IOException("番茄小说缓存 TXT 已不存在，请重新使用“番茄下载”。");
        }
        return new TomatoDownloadResult(normalizedBookId, record.title, txtPath);
    }

    public void cleanupBookCache(String bookId) throws IOException {
        String normalizedBookId = TomatoSourceLocation.normalizeBookId(bookId)
            .orElseThrow(() -> new IOException("Invalid Tomato book id: " + bookId));
        Files.createDirectories(libraryDir);

        StoredTomatoCache cache = readCache();
        StoredTomatoBook record = cache.books.remove(normalizedBookId);
        if (record != null && record.txtPath != null && !record.txtPath.isBlank()) {
            Path txtPath = Path.of(record.txtPath).toAbsolutePath().normalize();
            if (isInsideDataDir(txtPath) && Files.isRegularFile(txtPath)) {
                Files.deleteIfExists(txtPath);
            }
        }

        try (Stream<Path> stream = Files.list(libraryDir)) {
            for (Path path : stream.toList()) {
                String fileName = path.getFileName().toString();
                if (fileName.startsWith(normalizedBookId + "_")) {
                    deleteInsideDataDir(path);
                }
            }
        }
        writeCache(cache);
    }

    Path getLibraryDir() {
        return libraryDir;
    }

    private TomatoDownloadResult downloadInternal(String input, String expectedBookId) throws IOException {
        String trimmedInput = input == null ? "" : input.trim();
        if (trimmedInput.isEmpty()) {
            throw new IOException("番茄小说 ID 或链接不能为空。");
        }

        ensureServer();
        Instant startedAt = Instant.now();
        CreatedJob createdJob = createJob(trimmedInput);
        JobInfo job = waitForJob(createdJob.id());
        String resolvedBookId = expectedBookId == null ? createdJob.bookId() : expectedBookId;
        String title = firstNonBlank(job.title(), createdJob.bookId(), resolvedBookId);
        Path txtPath = locateDownloadedTxt(title, startedAt);

        StoredTomatoCache cache = readCache();
        StoredTomatoBook record = new StoredTomatoBook();
        record.bookId = resolvedBookId;
        record.title = title;
        record.txtPath = txtPath.toAbsolutePath().normalize().toString();
        record.updatedAt = System.currentTimeMillis();
        cache.books.put(resolvedBookId, record);
        writeCache(cache);

        return new TomatoDownloadResult(resolvedBookId, title, txtPath);
    }

    private void ensureServer() throws IOException {
        Files.createDirectories(dataDir);
        Files.createDirectories(libraryDir);
        writeConfig();

        if (serverBaseUri != null && isServerAlive()) {
            return;
        }
        if (externalServer) {
            throw new IOException("Tomato downloader Web UI is not available: " + serverBaseUri);
        }
        if (sidecarProcess != null && sidecarProcess.isAlive() && isServerAlive()) {
            return;
        }

        Path downloaderPath = downloaderResolver.resolve()
            .filter(Files::isRegularFile)
            .orElseThrow(() -> new IOException(
                "No bundled Tomato-Novel-Downloader is available for this system. Please choose a Windows x64 executable manually."
            ));
        int port = reserveLocalPort();
        serverBaseUri = URI.create("http://127.0.0.1:" + port);
        Path logFile = dataDir.resolve("tomato-downloader.log");
        sidecarProcess = sidecarLauncher.start(downloaderPath, dataDir, port, logFile);
        waitForServer();
    }

    private boolean isServerAlive() {
        if (serverBaseUri == null) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(serverBaseUri.resolve("/api/status"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private void stopSidecarProcess() {
        if (externalServer) {
            return;
        }

        Process process = sidecarProcess;
        sidecarProcess = null;
        serverBaseUri = null;
        if (process == null || !process.isAlive()) {
            return;
        }

        process.destroy();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private void waitForServer() throws IOException {
        long deadline = System.currentTimeMillis() + startTimeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (isServerAlive()) {
                return;
            }
            if (sidecarProcess != null && !sidecarProcess.isAlive()) {
                throw new IOException("Tomato downloader exited before Web UI became available.");
            }
            sleep(pollIntervalMillis);
        }
        throw new IOException("Tomato downloader Web UI did not start in time.");
    }

    private CreatedJob createJob(String input) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("book_id", input);
        JsonObject response = sendJson("POST", "/api/jobs", payload);
        long id = requireLong(response, "id");
        String bookId = requireString(response, "book_id");
        return new CreatedJob(id, bookId);
    }

    private JobInfo waitForJob(long jobId) throws IOException {
        long deadline = System.currentTimeMillis() + jobTimeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            JobInfo job = fetchJob(jobId);
            if (job.hasFormatOptions()) {
                submitChoice(jobId, "format", "txt");
                sleep(pollIntervalMillis);
                continue;
            }
            if (job.hasBookNameOptions()) {
                submitChoice(jobId, "book_name", null);
                sleep(pollIntervalMillis);
                continue;
            }
            switch (job.state()) {
                case "done" -> {
                    return job;
                }
                case "failed" -> throw new IOException("Tomato download failed: " + firstNonBlank(job.message(), "unknown error"));
                case "canceled" -> throw new IOException("Tomato download was canceled.");
                default -> sleep(pollIntervalMillis);
            }
        }
        throw new IOException("Tomato download timed out.");
    }

    private JobInfo fetchJob(long jobId) throws IOException {
        JsonObject response = sendJson("GET", "/api/jobs?id=" + jobId, null);
        JsonArray items = response.getAsJsonArray("items");
        if (items == null || items.isEmpty()) {
            throw new IOException("Tomato download job not found: " + jobId);
        }
        JsonObject job = items.get(0).getAsJsonObject();
        return new JobInfo(
            requireString(job, "state"),
            optionalString(job, "title").orElse(null),
            optionalString(job, "message").orElse(null),
            hasNonEmptyArray(job, "book_name_options"),
            hasNonEmptyArray(job, "format_options")
        );
    }

    private void submitChoice(long jobId, String choiceType, String value) throws IOException {
        JsonObject payload = new JsonObject();
        if (value == null) {
            payload.add("value", JsonNull.INSTANCE);
        } else {
            payload.addProperty("value", value);
        }
        sendJson("POST", "/api/jobs/" + jobId + "/" + choiceType, payload);
    }

    private JsonObject sendJson(String method, String path, JsonObject payload) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(serverBaseUri.resolve(path))
            .timeout(HTTP_TIMEOUT)
            .header("Accept", "application/json");
        if ("POST".equals(method)) {
            builder.header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Tomato downloader request was interrupted.", ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Tomato downloader request failed with status " + response.statusCode() + ": " + response.body());
        }
        JsonElement parsed = JsonParser.parseString(response.body());
        if (!parsed.isJsonObject()) {
            throw new IOException("Tomato downloader returned invalid JSON.");
        }
        return parsed.getAsJsonObject();
    }

    private Path locateDownloadedTxt(String title, Instant startedAt) throws IOException {
        Files.createDirectories(libraryDir);
        List<TxtCandidate> candidates = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(libraryDir)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt")) {
                    continue;
                }
                FileTime modified = Files.getLastModifiedTime(path);
                int score = scoreCandidate(path, title, modified.toInstant(), startedAt);
                candidates.add(new TxtCandidate(path.toAbsolutePath().normalize(), modified.toMillis(), score));
            }
        }
        return candidates.stream()
            .filter(candidate -> candidate.score() > 0)
            .max(Comparator.comparingInt(TxtCandidate::score).thenComparingLong(TxtCandidate::modifiedMillis))
            .or(() -> candidates.stream().max(Comparator.comparingLong(TxtCandidate::modifiedMillis)))
            .map(TxtCandidate::path)
            .orElseThrow(() -> new IOException("Tomato downloader did not produce a TXT file."));
    }

    private int scoreCandidate(Path path, String title, Instant modifiedAt, Instant startedAt) {
        int score = modifiedAt.isAfter(startedAt.minusSeconds(2)) ? 1 : 0;
        String normalizedTitle = normalizeName(title);
        String normalizedFileName = normalizeName(stripExtension(path.getFileName().toString()));
        if (!normalizedTitle.isEmpty()
            && (normalizedFileName.contains(normalizedTitle) || normalizedTitle.contains(normalizedFileName))) {
            score += 4;
        }
        return score;
    }

    private StoredTomatoCache readCache() throws IOException {
        if (!Files.isRegularFile(metadataFile)) {
            return new StoredTomatoCache();
        }
        String json = Files.readString(metadataFile, StandardCharsets.UTF_8);
        StoredTomatoCache cache = GSON.fromJson(json, StoredTomatoCache.class);
        if (cache == null) {
            return new StoredTomatoCache();
        }
        if (cache.books == null) {
            cache.books = new HashMap<>();
        }
        return cache;
    }

    private void writeCache(StoredTomatoCache cache) throws IOException {
        Files.createDirectories(dataDir);
        Files.writeString(metadataFile, GSON.toJson(cache), StandardCharsets.UTF_8);
    }

    private void writeConfig() throws IOException {
        String savePath = yamlPath(libraryDir);
        String content = """
            old_cli: false
            max_workers: 1
            request_timeout: 15
            max_retries: 3
            max_wait_time: 1200
            min_wait_time: 1000
            novel_format: txt
            bulk_files: false
            auto_clear_dump: true
            auto_open_downloaded_files: false
            enable_audiobook: false
            save_path: "%s"
            use_official_api: true
            api_endpoints: []
            allow_overwrite_files: true
            ask_format_after_download: false
            """.formatted(savePath);
        Files.createDirectories(dataDir);
        Files.writeString(dataDir.resolve("config.yml"), content, StandardCharsets.UTF_8);
    }

    private void deleteInsideDataDir(Path target) throws IOException {
        Path normalized = target.toAbsolutePath().normalize();
        if (!isInsideDataDir(normalized) || normalized.equals(dataDir)) {
            throw new IOException("Refusing to delete unexpected Tomato cache path: " + normalized);
        }
        if (!Files.exists(normalized)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(normalized)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                if (!isInsideDataDir(path)) {
                    throw new IOException("Refusing to delete unexpected Tomato cache path: " + path);
                }
                Files.deleteIfExists(path);
            }
        }
    }

    private boolean isInsideDataDir(Path path) {
        return path.toAbsolutePath().normalize().startsWith(dataDir);
    }

    private int reserveLocalPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private void sleep(long millis) throws IOException {
        try {
            Thread.sleep(Math.max(1L, millis));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Tomato download was interrupted.", ex);
        }
    }

    private static Path defaultDataDir() {
        return Path.of(PathManager.getSystemPath(), "FishNovel", "tomato");
    }

    private static String yamlPath(Path path) {
        return path.toAbsolutePath().normalize().toString()
            .replace('\\', '/')
            .replace("\"", "\\\"");
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", "")
            .replaceAll("[\\\\/:*?\"<>|.。·_\\-]", "");
    }

    private static String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static long requireLong(JsonObject object, String key) throws IOException {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IOException("Tomato downloader response is missing `" + key + "`.");
        }
        return element.getAsLong();
    }

    private static String requireString(JsonObject object, String key) throws IOException {
        return optionalString(object, key)
            .orElseThrow(() -> new IOException("Tomato downloader response is missing `" + key + "`."));
    }

    private static Optional<String> optionalString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        String value = element.getAsString();
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static boolean hasNonEmptyArray(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() && !element.getAsJsonArray().isEmpty();
    }

    record CreatedJob(long id, String bookId) {
    }

    record JobInfo(String state, String title, String message, boolean hasBookNameOptions, boolean hasFormatOptions) {
    }

    record TxtCandidate(Path path, long modifiedMillis, int score) {
    }

    interface SidecarLauncher {
        Process start(Path executable, Path dataDir, int port, Path logFile) throws IOException;
    }

    private static final class ProcessSidecarLauncher implements SidecarLauncher {
        @Override
        public Process start(Path executable, Path dataDir, int port, Path logFile) throws IOException {
            ProcessBuilder builder = new ProcessBuilder(
                executable.toString(),
                "--server",
                "--data-dir",
                dataDir.toString()
            );
            builder.environment().put("TOMATO_WEB_ADDR", "127.0.0.1:" + port);
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
            return builder.start();
        }
    }

    private static final class FixedTomatoDownloaderResolver implements TomatoDownloaderResolver {
        private final Path downloaderPath;

        private FixedTomatoDownloaderResolver(Path downloaderPath) {
            this.downloaderPath = downloaderPath == null ? null : downloaderPath.toAbsolutePath().normalize();
        }

        @Override
        public Optional<Path> resolve() {
            return Optional.ofNullable(downloaderPath);
        }

        @Override
        public void setExternalPath(Path path) {
            throw new IllegalStateException("Tomato downloader path storage is not available.");
        }
    }

    private static final class StoredTomatoCache {
        private Map<String, StoredTomatoBook> books = new HashMap<>();
    }

    private static final class StoredTomatoBook {
        private String bookId;
        private String title;
        private String txtPath;
        private long updatedAt;
    }
}
