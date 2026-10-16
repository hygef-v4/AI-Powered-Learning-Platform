package edu.aiplatform.backup.infrastructure;

import edu.aiplatform.backup.application.IBackupStore;
import edu.aiplatform.files.application.StorageUnavailableException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Stream;

/** Thư mục backup local cho máy dev: {@code <dir>/<runId>/archive.alapbk}. */
public class LocalBackupStore implements IBackupStore {

    static final String ARCHIVE = "archive.alapbk";

    private final Path root;

    public LocalBackupStore(Path root) {
        this.root = root;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public StoredArchive put(UUID runId, Path archive) {
        Path dir = root.resolve(runId.toString());
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(ARCHIVE);
            Files.copy(archive, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredArchive(runId.toString(), Files.size(target), md5(target));
        } catch (IOException e) {
            throw new StorageUnavailableException("Không ghi được backup local", e);
        }
    }

    @Override
    public void delete(String runId) {
        Path dir = root.resolve(UUID.fromString(runId).toString());
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new StorageUnavailableException("Không xóa được backup local", e);
        }
    }

    static String md5(Path file) throws IOException {
        try (DigestInputStream in = new DigestInputStream(Files.newInputStream(file), MessageDigest.getInstance("MD5"))) {
            in.transferTo(java.io.OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(in.getMessageDigest().digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    InputStream open(UUID runId) throws IOException {
        return Files.newInputStream(root.resolve(runId.toString()).resolve(ARCHIVE));
    }
}
