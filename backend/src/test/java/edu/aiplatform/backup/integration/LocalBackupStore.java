package edu.aiplatform.backup.integration;

import edu.aiplatform.backup.service.IBackupStore;
import edu.aiplatform.files.service.StorageUnavailableException;
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

/** Thư mục backup local chỉ dùng trong kiểm thử: {@code <dir>/<runId>/archive.alapbk}. Ứng dụng chỉ lưu backup trên Drive. */
public class LocalBackupStore implements IBackupStore {

    static final String ARCHIVE = "archive.alapbk";

    private final Path root;

    public LocalBackupStore(Path root) {
        this.root = root;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public StoredArchive putArchive(UUID runId, Path archive) {
        Path dir = root.resolve(runId.toString());
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(ARCHIVE);
            Files.copy(archive, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredArchive(runId.toString(), Files.size(target), hashMd5(target));
        } catch (IOException e) {
            throw new StorageUnavailableException("Không ghi được backup local", e);
        }
    }

    @Override
    public void deleteArchive(String runId) {
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

    @Override
    public StoredArchive putWal(String name, Path file) {
        Path dir = root.resolve("wal");
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredArchive(name, Files.size(target), hashMd5(target));
        } catch (IOException e) {
            throw new StorageUnavailableException("Không ghi được WAL local", e);
        }
    }

    @Override
    public void deleteWal(String ref) {
        try {
            Files.deleteIfExists(root.resolve("wal").resolve(ref));
        } catch (IOException e) {
            throw new StorageUnavailableException("Không xóa được WAL local", e);
        }
    }

    static String hashMd5(Path file) throws IOException {
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
