package co.casterlabs.kv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kv.util.EnvHelper;
import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class KV {
    private static final Path ROOT = Paths.get(
        EnvHelper.string("KV_ROOT", "./data")
    ).toAbsolutePath().normalize();

    private static final Pattern VALID_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9%._-]+$");

    static {
        Thread.ofVirtual()
            .start(() -> {
                try {
                    while (true) {
                        try {
                            // Side effect of enumerate() is that it will clean up
                            // expired entries when it calls get().
                            KV.enumerate();
                        } catch (IOException ignored) {}

                        TimeUnit.MINUTES.sleep(5);
                    }
                } catch (InterruptedException e) {}
            });
    }

    public static void init() throws IOException {
        Files.createDirectories(ROOT);
    }

    public static boolean isValidKey(String key) {
        return VALID_KEY_PATTERN.matcher(key).matches();
    }

    private static Path resolveNoSlip(String path) throws IOException {
        Path resolved = ROOT.resolve(path).normalize();

        if (!resolved.startsWith(ROOT)) {
            throw new IllegalArgumentException("Path must be within the root directory.");
        }

        return resolved;
    }

    public static synchronized KVEntry get(@NonNull String key) throws IOException {
        Path path = resolveNoSlip(key);

        if (!path.toFile().exists()) {
            return null;
        }

        KVEntry entry = new KVEntry(path, key);

        // Auto-cleanup hasn't reached this entry yet.
        if (entry.isExpired()) {
            entry.delete();
            FastLogger.logStatic("Removed expired entry: %s", key);
            return null;
        }

        return entry;
    }

    public static List<KVEntry> enumerate() throws IOException {
        // This method is intentionally not synchronized, as we want to allow get()s to
        // be inter-leaved. We do guarantee atomicity by calling get() for each entry,
        // which will auto-cleanup expired entries as we go.

        List<KVEntry> entries = new ArrayList<>();
        if (!ROOT.toFile().exists()) {
            return entries;
        }

        try (Stream<Path> stream = Files.walk(ROOT, 1)) {
            stream.forEach((path) -> {
                if (path.equals(ROOT)) {
                    return;
                }

                String key = ROOT.relativize(path).toString();

                try {
                    KVEntry entry = get(key);
                    if (entry != null) {
                        entries.add(entry);
                    }
                } catch (IOException e) {
                    FastLogger.logException(new IOException("Failed to read entry: " + key + ". Ignoring...", e));
                }
            });
        }

        return entries;
    }

    public static synchronized void put(@NonNull String key, @Nullable String contentType, long ttl, @NonNull InputStream data) throws IOException {
        Path path = resolveNoSlip(key);
        long expiresAt = ttl == -1 ? -1 : System.currentTimeMillis() + (ttl * 1000);

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        Files.createDirectories(path);
        Files.writeString(path.resolve("content-type"), contentType, StandardCharsets.UTF_8);
        Files.writeString(path.resolve("expires-at"), Long.toString(expiresAt), StandardCharsets.UTF_8);

        try (FileOutputStream out = new FileOutputStream(path.resolve("data").toFile())) {
            data.transferTo(out);
        }
    }

    @RequiredArgsConstructor
    public static class KVEntry {
        private final Path path;
        public final String key;

        private String contentTypeCached = null;
        private long expiresAtCached = -1;

        /**
         * @return -1 if it doesn't expire.
         */
        public long expiresAt() throws IOException {
            if (this.expiresAtCached == -1) {
                Path ttlPath = this.path.resolve("expires-at");
                this.expiresAtCached = Long.parseLong(Files.readString(ttlPath, StandardCharsets.UTF_8));
            }

            return this.expiresAtCached;
        }

        public String contentType() throws IOException {
            if (this.contentTypeCached == null) {
                Path contentTypePath = this.path.resolve("content-type");
                this.contentTypeCached = Files.readString(contentTypePath, StandardCharsets.UTF_8);
            }

            return this.contentTypeCached;
        }

        public boolean isExpired() throws IOException {
            long expiresAt = this.expiresAt();
            return expiresAt != -1 && System.currentTimeMillis() > expiresAt;
        }

        public long lastModified() {
            return this.path.toFile().lastModified();
        }

        public File data() {
            return this.path.resolve("data").toFile();
        }

        public void delete() throws IOException {
            Files.deleteIfExists(this.path.resolve("expires-at"));
            Files.deleteIfExists(this.path.resolve("content-type"));
            Files.deleteIfExists(this.path.resolve("data"));
            Files.deleteIfExists(this.path);
        }

        public JsonObject toJson() throws IOException {
            return new JsonObject()
                .put("key", this.key)
                .put("expiresAt", this.expiresAt())
                .put("contentType", this.contentType())
                .put("lastModified", this.lastModified());
        }

    }

}
