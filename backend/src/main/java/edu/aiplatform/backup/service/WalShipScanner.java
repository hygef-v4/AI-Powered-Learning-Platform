package edu.aiplatform.backup.service;

import edu.aiplatform.backup.config.BackupProperties;
import edu.aiplatform.backup.repository.WalSegmentRepository;
import edu.aiplatform.files.service.StorageUnavailableException;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.settings.contract.ISettings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đẩy WAL lên Google Drive (U03, 2026-11-26). Postgres chép tệp WAL đã ghi xong vào thư mục WAL archive (chốt tối đa
 * mỗi 5 phút); mỗi phút worker nén gzip, mã hóa nếu có khóa, tải lên thư mục {@code wal} trên Drive, đối chiếu kích
 * thước và MD5, ghi {@code wal_segments} rồi xóa tệp trên máy. Lỗi lưu trữ thì giữ tệp, thử lại ở phút sau. Không cấu
 * hình Drive hoặc đang tắt "Bật sao lưu" thì xóa tệp, không lưu ở đâu cả (WAL không dùng được khi không có base backup).
 */
@Component
public class WalShipScanner implements IScheduledScanner {

    private static final Logger log = LoggerFactory.getLogger(WalShipScanner.class);

    /** Tệp WAL (24 ký tự hex), tệp lịch sử timeline ({@code .history}) và nhãn backup ({@code .backup}). */
    static final Pattern WAL_FILE = Pattern.compile("^[0-9A-F]{24}(\\.partial|\\.[0-9A-F]{8}\\.backup)?$|^[0-9A-F]{8}\\.history$");
    static final int MAX_FILES_PER_SCAN = 100;
    static final String DEFAULT_WAL_DIR = "/wal-archive";

    private final BackupProperties properties;
    private final IBackupStore store;
    private final WalSegmentRepository segments;
    private final ISettings settings;

    public WalShipScanner(BackupProperties properties, IBackupStore store, WalSegmentRepository segments,
                          ISettings settings) {
        this.properties = properties;
        this.store = store;
        this.segments = segments;
        this.settings = settings;
    }

    @Override
    public String name() {
        return "backup-wal-ship";
    }

    @Override
    @Transactional
    public void scan(Instant now) {
        Path dir = resolveWalDir();
        if (!Files.isDirectory(dir) || !segments.tryLockShipping()) {
            return;
        }
        List<Path> files = listReadyFiles(dir);
        if (files.isEmpty()) {
            return;
        }
        if (!store.isAvailable() || !settings.get(BackupSettingDefinitions.ENABLED).orElse(false)) {
            files.forEach(WalShipScanner::deleteQuietly);
            return;
        }
        PublicKey publicKey = BackupKeys.loadOptionalPublicKey(properties.publicKey());
        Path work = resolveWorkDir();
        for (Path file : files) {
            try {
                shipFile(file, work, publicKey, now);
            } catch (StorageUnavailableException | IOException e) {
                log.warn("Chưa đẩy được WAL {} lên Drive, thử lại ở lần quét sau", file.getFileName());
                return;
            }
        }
    }

    private void shipFile(Path file, Path work, PublicKey publicKey, Instant now) throws IOException {
        Files.createDirectories(work);
        String name = file.getFileName() + (publicKey == null ? ".gz" : ".gz.alapbk");
        Path compressed = work.resolve(file.getFileName() + ".gz");
        Path archive = work.resolve(name);
        try {
            compressFile(file, compressed);
            BackupJobHandler.encryptArchiveFile(compressed, archive, publicKey);
            IBackupStore.StoredArchive stored = store.putWal(name, archive);
            long size = Files.size(archive);
            if (stored.byteSize() != size
                    || (stored.md5() != null && !stored.md5().equalsIgnoreCase(BackupJobHandler.hashMd5(archive)))) {
                store.deleteWal(stored.ref());
                throw new StorageUnavailableException("Tệp WAL tải lên không khớp kích thước hoặc checksum", null);
            }
            segments.recordUploaded(file.getFileName().toString(), stored.ref(), size, now);
            Files.deleteIfExists(file);
        } finally {
            Files.deleteIfExists(compressed);
            Files.deleteIfExists(archive);
        }
    }

    /** Tệp đã chép xong (bỏ qua {@code .tmp} đang ghi), theo thứ tự tên để WAL lên Drive đúng thứ tự. */
    static List<Path> listReadyFiles(Path dir) {
        try (Stream<Path> paths = Files.list(dir)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> WAL_FILE.matcher(path.getFileName().toString()).matches())
                    .sorted()
                    .limit(MAX_FILES_PER_SCAN)
                    .toList();
        } catch (IOException e) {
            log.warn("Không đọc được thư mục WAL archive");
            return List.of();
        }
    }

    static void compressFile(Path source, Path target) throws IOException {
        try (InputStream in = Files.newInputStream(source);
             OutputStream out = new GZIPOutputStream(Files.newOutputStream(target))) {
            in.transferTo(out);
        }
    }

    private Path resolveWalDir() {
        return Path.of(properties.walDir() == null || properties.walDir().isBlank()
                ? DEFAULT_WAL_DIR : properties.walDir());
    }

    private Path resolveWorkDir() {
        String tmp = properties.tmpDir() == null || properties.tmpDir().isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "platform-backup").toString() : properties.tmpDir();
        return Path.of(tmp, "wal");
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Không xóa được tệp WAL {}", file.getFileName());
        }
    }
}
